package no.nav.syfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.auth.Credentials
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.prometheus.client.hotspot.DefaultExports
import java.io.FileInputStream
import java.util.concurrent.TimeUnit
import no.nav.syfo.bucket.BucketService
import no.nav.syfo.db.Database
import no.nav.syfo.kafka.aiven.KafkaUtils
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.legeerklaering.LegeerklaeringsService
import no.nav.syfo.nais.isalive.naisIsAliveRoute
import no.nav.syfo.nais.isready.naisIsReadyRoute
import no.nav.syfo.nais.prometheus.naisPrometheusRoute
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val objectMapper: ObjectMapper =
    ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

val log: Logger = LoggerFactory.getLogger("no.nav.no.nav.syfo.pale2register")

fun main() {
    val embeddedServer =
        embeddedServer(
            Netty,
            port = EnvironmentVariables().applicationPort,
            module = Application::module,
        )
    Runtime.getRuntime()
        .addShutdownHook(
            Thread {
                embeddedServer.stop(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(10))
            },
        )
    embeddedServer.start(true)
}

fun Application.configureRouting(applicationState: ApplicationState) {
    routing {
        naisIsAliveRoute(applicationState)
        naisIsReadyRoute(applicationState)
        naisPrometheusRoute()
    }
}

fun Application.module() {
    val environmentVariables = EnvironmentVariables()
    val applicationState = ApplicationState()
    val database = Database(environmentVariables)

    monitor.subscribe(ApplicationStopped) {
        applicationState.ready = false
        applicationState.alive = false
    }

    configureRouting(applicationState = applicationState)

    DefaultExports.initialize()

    val paleStorageCredentials: Credentials =
        GoogleCredentials.fromStream(FileInputStream("/var/run/secrets/pale2-google-creds.json"))

    val bucketStorage: Storage =
        StorageOptions.newBuilder().setCredentials(paleStorageCredentials).build().service
    val bucketService = BucketService(environmentVariables.legeerklaeringBucketName, bucketStorage)

    val aivenConfig =
        KafkaUtils.getAivenKafkaConfig()
            .toConsumerConfig(
                "${environmentVariables.applicationName}-consumer",
                valueDeserializer = StringDeserializer::class,
            )
            .also { it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "none" }
    val aivenKafkaConsumer = KafkaConsumer<String, String>(aivenConfig)

    LegeerklaeringsService(
            environmentVariables,
            applicationState,
            aivenKafkaConsumer,
            bucketService,
            database
        )
        .run()
}

data class ApplicationState(
    var alive: Boolean = true,
    var ready: Boolean = true,
)
