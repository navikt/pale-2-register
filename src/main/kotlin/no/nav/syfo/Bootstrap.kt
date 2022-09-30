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
import no.nav.syfo.gcp.BucketService
import no.nav.syfo.kafka.aiven.KafkaUtils
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.model.LegeerklaeringSak
import no.nav.syfo.model.kafka.LegeerklaeringKafkaMessage
import no.nav.syfo.persistering.db.slettLegeerklaering
import no.nav.syfo.persistering.handleRecivedMessage
import no.nav.syfo.utils.LoggingMeta
import no.nav.syfo.utils.TrackableException
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.time.Duration
import no.nav.syfo.persistering.db.hentMsgId

val objectMapper: ObjectMapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    .registerKotlinModule()
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

val log: Logger = LoggerFactory.getLogger("no.nav.no.nav.syfo.pale2register")

@DelicateCoroutinesApi
fun main() {
    val env = Environment()
    val database = Database(env)

    val applicationState = ApplicationState()

    val applicationEngine = createApplicationEngine(env, applicationState)

    DefaultExports.initialize()

    val paleStorageCredentials: Credentials =
        GoogleCredentials.fromStream(FileInputStream("/var/run/secrets/pale2-google-creds.json"))

    val bucketStorage: Storage = StorageOptions.newBuilder().setCredentials(paleStorageCredentials).build().service
    val bucketService = BucketService(env.legeerklaeringBucketName, bucketStorage)

    val aivenConfig = KafkaUtils.getAivenKafkaConfig().toConsumerConfig(
        "${env.applicationName}-consumer", valueDeserializer = StringDeserializer::class
    ).also { it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "none" }
    val aivenKafkaConsumer = KafkaConsumer<String, String>(aivenConfig)

    launchListeners(env, applicationState, aivenKafkaConsumer, bucketService, database)

    ApplicationServer(applicationEngine, applicationState).start()
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
            applicationState.ready = false
            applicationState.alive = false
        }
    }

@DelicateCoroutinesApi
fun launchListeners(
    env: Environment,
    applicationState: ApplicationState,
    aivenKafkaConsumer: KafkaConsumer<String, String>,
    bucketService: BucketService,
    database: Database
) {
    createListener(applicationState) {
        aivenKafkaConsumer.subscribe(listOf(env.legeerklaringTopic))
        blockingApplicationLogic(
            aivenKafkaConsumer,
            bucketService,
            applicationState,
            database
        )
    }
}

suspend fun blockingApplicationLogic(
    aivenKafkaConsumer: KafkaConsumer<String, String>,
    bucketService: BucketService,
    applicationState: ApplicationState,
    database: Database
) {
    while (applicationState.ready) {
        aivenKafkaConsumer.poll(Duration.ofSeconds(10))
            .forEach { consumerRecord ->
                if (consumerRecord.value() == null) {
                    val legeerklaeringId = consumerRecord.key()
                    log.info("Mottatt tombstone-melding for legeerklæring med id $legeerklaeringId")
                    val msgId = database.hentMsgId(legeerklaeringId)
                    if (msgId.isNullOrEmpty()) {
                        log.error("Slettet ikkje legeerklæring grunnet: fant ikkje msgId for legeerklæring med id $legeerklaeringId")
                    } else {
                        database.slettLegeerklaering(legeerklaeringId)
                        bucketService.deleteLegeerklaring(msgId)
                        log.info("Slettet legeerklæring med id $legeerklaeringId")
                    }
                } else {
                    val legeerklaeringKafkaMessage: LegeerklaeringKafkaMessage =
                        objectMapper.readValue(consumerRecord.value())
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
