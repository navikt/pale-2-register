package no.nav.syfo.legeerklaering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration
import kotlinx.coroutines.runBlocking
import no.nav.syfo.ApplicationState
import no.nav.syfo.EnvironmentVariables
import no.nav.syfo.bucket.BucketService
import no.nav.syfo.db.TestDB
import no.nav.syfo.db.dropData
import no.nav.syfo.db.erLegeerklaeringsopplysningerLagret
import no.nav.syfo.db.lagreMottattLegeerklearing
import no.nav.syfo.model.LegeerklaeringSak
import no.nav.syfo.model.Status
import no.nav.syfo.model.ValidationResult
import no.nav.syfo.model.kafka.LegeerklaeringKafkaMessage
import no.nav.syfo.objectMapper
import no.nav.syfo.util.receivedLegeerklaering
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LegeerklaeringsServiceTest {
    private val env = mockk<EnvironmentVariables>(relaxed = true)
    private val applicationState = ApplicationState()
    private val aivenKafkaConsumer = mockk<KafkaConsumer<String, String>>()
    private val bucketService = mockk<BucketService>()
    private val database = TestDB.database

    private val legeerklaeringsService =
        LegeerklaeringsService(env, applicationState, aivenKafkaConsumer, bucketService, database)

    @BeforeEach
    fun setup() {
        database.connection.dropData()
        applicationState.ready = true
        applicationState.alive = true
    }

    @Test
    internal fun `motta legeerklaering OK`() {
        every { aivenKafkaConsumer.subscribe(any<List<String>>()) } returns Unit

        val kafkaMessage =
            objectMapper.writeValueAsString(
                LegeerklaeringKafkaMessage(
                    "12314",
                    ValidationResult(Status.OK, emptyList()),
                    emptyList()
                )
            )
        val records =
            mapOf<TopicPartition, List<ConsumerRecord<String, String>>>(
                TopicPartition("Uansett", 42) to
                    listOf(
                        ConsumerRecord("", 17, 23, "12314", kafkaMessage),
                    ),
            )
        val consumerRecords = ConsumerRecords(records)

        every { bucketService.getLegeerklaring("12314") } returns receivedLegeerklaering

        every { aivenKafkaConsumer.poll(any<Duration>()) } answers
            {
                applicationState.ready = false
                consumerRecords
            }

        runBlocking {
            val job = legeerklaeringsService.run()

            job.join()

            verify(exactly = 1) { aivenKafkaConsumer.subscribe(any<List<String>>()) }

            verify(exactly = 1) { aivenKafkaConsumer.poll(any<Duration>()) }

            verify(exactly = 1) { bucketService.getLegeerklaring("12314") }
            assertEquals(true, database.erLegeerklaeringsopplysningerLagret("12314"))
        }
    }

    @Test
    internal fun `Slette legeerklaering`() {
        every { aivenKafkaConsumer.subscribe(any<List<String>>()) } returns Unit

        database.lagreMottattLegeerklearing(
            LegeerklaeringSak(
                receivedLegeerklaering = receivedLegeerklaering,
                validationResult = ValidationResult(Status.OK, emptyList()),
                vedlegg = emptyList(),
            ),
        )

        val records =
            mapOf<TopicPartition, List<ConsumerRecord<String, String>>>(
                TopicPartition("Uansett", 42) to
                    listOf(
                        ConsumerRecord("", 17, 23, "12314", null),
                    ),
            )
        val consumerRecords = ConsumerRecords(records)

        every { bucketService.deleteLegeerklaring(receivedLegeerklaering.msgId) } returns Unit

        every { aivenKafkaConsumer.poll(any<Duration>()) } answers
            {
                applicationState.ready = false
                consumerRecords
            }

        runBlocking {
            val job = legeerklaeringsService.run()

            job.join()

            verify(exactly = 1) { aivenKafkaConsumer.subscribe(any<List<String>>()) }

            verify(exactly = 1) { aivenKafkaConsumer.poll(any<Duration>()) }

            verify(exactly = 0) { bucketService.getLegeerklaring("12314") }

            verify(exactly = 1) { bucketService.deleteLegeerklaring(receivedLegeerklaering.msgId) }
            assertEquals(false, database.erLegeerklaeringsopplysningerLagret("12314"))
        }
    }
}
