package no.nav.syfo.legeerklaering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.Environment
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.gcp.BucketService
import no.nav.syfo.model.LegeerklaeringSak
import no.nav.syfo.model.Status
import no.nav.syfo.model.ValidationResult
import no.nav.syfo.model.kafka.LegeerklaeringKafkaMessage
import no.nav.syfo.objectMapper
import no.nav.syfo.persistering.db.erLegeerklaeringsopplysningerLagret
import no.nav.syfo.persistering.db.lagreMottattLegeerklearing
import no.nav.syfo.util.TestDB
import no.nav.syfo.util.dropData
import no.nav.syfo.util.receivedLegeerklaering
import no.nav.syfo.util.validationResult
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class LegeerklaeringsServiceTest {
    private val env = mockk<Environment>(relaxed = true)
    private val applicationState = ApplicationState()
    private val aivenKafkaConsumer = mockk<KafkaConsumer<String, String>>()
    private val bucketService = mockk<BucketService>()
    private val database = TestDB.database

    private val legeerklaeringsService = LegeerklaeringsService(env, applicationState, aivenKafkaConsumer, bucketService, database)



    @BeforeEach
    fun setup() {
        database.connection.dropData()
        applicationState.ready = true
        applicationState.alive = true
    }

    @Test
    internal fun `motta legeerklaering OK`() {
        every {
            aivenKafkaConsumer.subscribe(any<List<String>>())
        } returns Unit

        val kafkaMessage = objectMapper.writeValueAsString(LegeerklaeringKafkaMessage("12314", ValidationResult(Status.OK, emptyList()), emptyList()))
        val records = mapOf<TopicPartition, List<ConsumerRecord<String, String>>>(
            TopicPartition("Uansett", 42) to listOf(
                ConsumerRecord("", 17, 23, "12314", kafkaMessage)
            )
        )
        val consumerRecords = ConsumerRecords(records)

        every {
            bucketService.getLegeerklaring("12314")
        } returns receivedLegeerklaering

        every {
            aivenKafkaConsumer.poll(any<Duration>())
        } answers {
            applicationState.ready = false
            consumerRecords
        }

        runBlocking {
            val job = legeerklaeringsService.run()

            job.join()

            verify(exactly = 1) {
                aivenKafkaConsumer.subscribe(any<List<String>>())
            }

            verify(exactly = 1) {
                aivenKafkaConsumer.poll(any<Duration>())
            }

            verify(exactly = 1) {
                bucketService.getLegeerklaring("12314")
            }

            database.erLegeerklaeringsopplysningerLagret("12314") shouldBeEqualTo true
        }


    }
}