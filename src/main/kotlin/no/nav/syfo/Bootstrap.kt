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
import io.prometheus.client.hotspot.DefaultExports
import java.io.FileInputStream
import kotlinx.coroutines.DelicateCoroutinesApi
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.db.Database
import no.nav.syfo.gcp.BucketService
import no.nav.syfo.kafka.aiven.KafkaUtils
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.legeerklaering.LegeerklaeringsService
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

@DelicateCoroutinesApi
fun main() {
    val env = Environment()
    val database = Database(env)

    val applicationState = ApplicationState()

    val applicationEngine = createApplicationEngine(env, applicationState)

    DefaultExports.initialize()

    val paleStorageCredentials: Credentials =
        GoogleCredentials.fromStream(FileInputStream("/var/run/secrets/pale2-google-creds.json"))

    val bucketStorage: Storage =
        StorageOptions.newBuilder().setCredentials(paleStorageCredentials).build().service
    val bucketService = BucketService(env.legeerklaeringBucketName, bucketStorage)

    val aivenConfig =
        KafkaUtils.getAivenKafkaConfig()
            .toConsumerConfig(
                "${env.applicationName}-consumer",
                valueDeserializer = StringDeserializer::class,
            )
            .also { it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "none" }
    val aivenKafkaConsumer = KafkaConsumer<String, String>(aivenConfig)

    LegeerklaeringsService(env, applicationState, aivenKafkaConsumer, bucketService, database).run()

    ApplicationServer(applicationEngine, applicationState).start()
}
