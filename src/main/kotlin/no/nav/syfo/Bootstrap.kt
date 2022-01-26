package no.nav.syfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.auth.Credentials
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.db.Database
import no.nav.syfo.db.VaultCredentialService
import no.nav.syfo.gcp.BucketService
import no.nav.syfo.kafka.aiven.KafkaUtils
import no.nav.syfo.kafka.envOverrides
import no.nav.syfo.kafka.loadBaseConfig
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.model.LegeerklaeringSak
import no.nav.syfo.model.kafka.LegeerklaeringKafkaMessage
import no.nav.syfo.persistering.handleRecivedMessage
import no.nav.syfo.utils.LoggingMeta
import no.nav.syfo.utils.TrackableException
import no.nav.syfo.vault.RenewVaultService
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.time.Duration
import java.util.Properties

val objectMapper: ObjectMapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    .registerKotlinModule()
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

val log: Logger = LoggerFactory.getLogger("no.nav.no.nav.syfo.pale2register")

@DelicateCoroutinesApi
fun main() {
    val env = Environment()
    val vaultSecrets = VaultSecrets()

    val vaultCredentialService = VaultCredentialService()
    val database = Database(env, vaultCredentialService)

    val applicationState = ApplicationState()

    val applicationEngine = createApplicationEngine(env, applicationState)

    DefaultExports.initialize()

    ApplicationServer(applicationEngine, applicationState).start()

    val paleStorageCredentials: Credentials =
        GoogleCredentials.fromStream(FileInputStream("/var/run/secrets/nais.io/vault/pale2-google-creds.json"))

    val bucketStorage: Storage = StorageOptions.newBuilder().setCredentials(paleStorageCredentials).build().service
    val bucketService = BucketService(env.legeerklaeringBucketName, bucketStorage)

    val kafkaBaseConfig = loadBaseConfig(env, vaultSecrets).envOverrides()
    kafkaBaseConfig["auto.offset.reset"] = "none"
    val consumerConfig = kafkaBaseConfig.toConsumerConfig(
        "${env.applicationName}-consumer", valueDeserializer = StringDeserializer::class
    )

    val aivenConfig = KafkaUtils.getAivenKafkaConfig().toConsumerConfig(
        "${env.applicationName}-consumer", valueDeserializer = StringDeserializer::class
    ).also { it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest" }
    val aivenKafkaConsumer = KafkaConsumer<String, String>(aivenConfig)

    applicationState.ready = true

    if (!env.developmentMode) {
        RenewVaultService(vaultCredentialService, applicationState).startRenewTasks()
    }

    launchListeners(env, applicationState, consumerConfig, aivenKafkaConsumer, bucketService, database)
}

@DelicateCoroutinesApi
fun createListener(applicationState: ApplicationState, action: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch {
        try {
            action()
        } catch (e: TrackableException) {
            log.error(
                "En uhåndtert feil oppstod, applikasjonen restarter {}",
                fields(e.loggingMeta), e.cause
            )
        } finally {
            applicationState.alive = false
        }
    }

@DelicateCoroutinesApi
fun launchListeners(
    env: Environment,
    applicationState: ApplicationState,
    consumerProperties: Properties,
    aivenKafkaConsumer: KafkaConsumer<String, String>,
    bucketService: BucketService,
    database: Database
) {
    createListener(applicationState) {
        val kafkaLegeerklaeringSakconsumer = KafkaConsumer<String, String>(consumerProperties)
        kafkaLegeerklaeringSakconsumer.subscribe(listOf(env.pale2OkTopic, env.pale2AvvistTopic))
        applicationState.ready = true

        aivenKafkaConsumer.subscribe(listOf(env.legeerklaringTopic))

        blockingApplicationLogic(
            kafkaLegeerklaeringSakconsumer,
            aivenKafkaConsumer,
            bucketService,
            applicationState,
            database
        )
    }
}

suspend fun blockingApplicationLogic(
    kafkaLegeerklaeringSakconsumer: KafkaConsumer<String, String>,
    aivenKafkaConsumer: KafkaConsumer<String, String>,
    bucketService: BucketService,
    applicationState: ApplicationState,
    database: Database
) {
    while (applicationState.ready) {
        kafkaLegeerklaeringSakconsumer.poll(Duration.ofSeconds(10)).forEach { consumerRecord ->
            val legeerklaeringSak: LegeerklaeringSak = objectMapper.readValue(consumerRecord.value())
            handleLegeerklaringSak(legeerklaeringSak, database)
        }
        aivenKafkaConsumer.poll(Duration.ofSeconds(10))
            //.filter { !(it.headers().any { header -> header.value().contentEquals("macgyver".toByteArray()) }) }
            .forEach { consumerRecord ->
                val legeerklaeringKafkaMessage: LegeerklaeringKafkaMessage = objectMapper.readValue(consumerRecord.value())
                val receivedLegeerklaering =
                    bucketService.getLegeerklaring(legeerklaeringKafkaMessage.legeerklaeringObjectId)
                val legeerklaeringSak = LegeerklaeringSak(
                    receivedLegeerklaering,
                    legeerklaeringKafkaMessage.validationResult,
                    legeerklaeringKafkaMessage.vedlegg
                )
                handleLegeerklaringSak(legeerklaeringSak, database)
            }
    }
}

private suspend fun handleLegeerklaringSak(
    legeerklaeringSak: LegeerklaeringSak,
    database: Database
) {
    val loggingMeta = LoggingMeta(
        mottakId = legeerklaeringSak.receivedLegeerklaering.navLogId,
        orgNr = legeerklaeringSak.receivedLegeerklaering.legekontorOrgNr,
        msgId = legeerklaeringSak.receivedLegeerklaering.msgId,
        legeerklaeringId = legeerklaeringSak.receivedLegeerklaering.legeerklaering.id
    )
    handleRecivedMessage(
        legeerklaeringSak,
        loggingMeta,
        database
    )
}
