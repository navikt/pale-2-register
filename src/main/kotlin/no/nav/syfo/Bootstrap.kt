package no.nav.syfo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.util.KtorExperimentalAPI
import java.time.Duration
import java.util.Properties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.kafka.envOverrides
import no.nav.syfo.kafka.loadBaseConfig
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.model.LegeerklaeringSak
import no.nav.syfo.utils.LoggingMeta
import no.nav.syfo.utils.TrackableException
import no.nav.syfo.utils.getFileAsString
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val objectMapper: ObjectMapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    .registerKotlinModule()
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

val log: Logger = LoggerFactory.getLogger("no.nav.no.nav.syfo.pale2register")

@KtorExperimentalAPI
fun main() {
    val env = Environment()
    val vaultSecrets = VaultSecrets(
        serviceuserUsername = getFileAsString(env.serviceuserUsernamePath),
        serviceuserPassword = getFileAsString(env.serviceuserPasswordPath)
    )

    val applicationState = ApplicationState()

    val applicationEngine = createApplicationEngine(env, applicationState)

    ApplicationServer(applicationEngine).start()

    val kafkaBaseConfig = loadBaseConfig(env, vaultSecrets).envOverrides()
    val consumerConfig = kafkaBaseConfig.toConsumerConfig(
        "${env.applicationName}-consumer", valueDeserializer = StringDeserializer::class
    )

    launchListeners(env, applicationState, consumerConfig)
}

fun createListener(applicationState: ApplicationState, action: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch {
        try {
            action()
        } catch (e: TrackableException) {
            log.error("En uhåndtert feil oppstod, applikasjonen restarter {}",
                StructuredArguments.fields(e.loggingMeta), e.cause)
        } finally {
            applicationState.alive = false
        }
    }

@KtorExperimentalAPI
fun launchListeners(
    env: Environment,
    applicationState: ApplicationState,
    consumerProperties: Properties
) {
    createListener(applicationState) {
        val kafkaLegeerklaeringSakconsumer = KafkaConsumer<String, String>(consumerProperties)
        kafkaLegeerklaeringSakconsumer.subscribe(listOf(env.pale2OkTopic, env.pale2AvvistTopic))
        applicationState.ready = true

        blockingApplicationLogic(
            kafkaLegeerklaeringSakconsumer,
            applicationState
        )
    }
}

@KtorExperimentalAPI
suspend fun blockingApplicationLogic(
    kafkaLegeerklaeringSakconsumer: KafkaConsumer<String, String>,
    applicationState: ApplicationState
) {
    while (applicationState.ready) {
        kafkaLegeerklaeringSakconsumer.poll(Duration.ofMillis(0)).forEach { consumerRecord ->

            val legeerklaeringSak: LegeerklaeringSak = objectMapper.readValue(consumerRecord.value())

            val loggingMeta = LoggingMeta(
                mottakId = legeerklaeringSak.receivedLegeerklaering.navLogId,
                orgNr = legeerklaeringSak.receivedLegeerklaering.legekontorOrgNr,
                msgId = legeerklaeringSak.receivedLegeerklaering.msgId,
                legeerklaeringId = legeerklaeringSak.receivedLegeerklaering.legeerklaering.id
            )

            // TODO store i DB

            handleRecivedMessage(
                legeerklaeringSak,
                loggingMeta
            )
        }

        delay(100)
    }
}
