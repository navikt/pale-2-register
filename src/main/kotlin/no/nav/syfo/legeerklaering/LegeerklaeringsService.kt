package no.nav.syfo.legeerklaering

import com.fasterxml.jackson.module.kotlin.readValue
import java.lang.Exception
import java.time.Duration
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.Environment
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.gcp.BucketService
import no.nav.syfo.log
import no.nav.syfo.metrics.INCOMING_MESSAGE_COUNTER
import no.nav.syfo.metrics.MESSAGE_STORED_IN_DB_COUNTER
import no.nav.syfo.model.LegeerklaeringSak
import no.nav.syfo.model.kafka.LegeerklaeringKafkaMessage
import no.nav.syfo.objectMapper
import no.nav.syfo.persistering.db.erLegeerklaeringsopplysningerLagret
import no.nav.syfo.persistering.db.hentMsgId
import no.nav.syfo.persistering.db.lagreMottattLegeerklearing
import no.nav.syfo.persistering.db.slettLegeerklaering
import no.nav.syfo.utils.LoggingMeta
import no.nav.syfo.utils.TrackableException
import no.nav.syfo.utils.wrapExceptions
import org.apache.kafka.clients.consumer.KafkaConsumer

class LegeerklaeringsService(
    val env: Environment,
    val applicationState: ApplicationState,
    val aivenKafkaConsumer: KafkaConsumer<String, String>,
    val bucketService: BucketService,
    val database: DatabaseInterface,
) {

    @OptIn(DelicateCoroutinesApi::class)
    fun run(): Job {
        return GlobalScope.launch(Dispatchers.IO) {
            try {
                aivenKafkaConsumer.subscribe(listOf(env.legeerklaringTopic))
                while (applicationState.ready) {
                    aivenKafkaConsumer.poll(Duration.ofSeconds(10)).forEach { consumerRecord ->
                        if (consumerRecord.value() == null) {
                            val legeerklaeringId = consumerRecord.key()
                            log.info(
                                "Mottatt tombstone-melding for legeerklæring med id $legeerklaeringId"
                            )
                            val msgId = database.hentMsgId(legeerklaeringId)
                            if (msgId.isNullOrEmpty()) {
                                log.error(
                                    "Slettet ikkje legeerklæring grunnet: fant ikkje msgId for legeerklæring med id $legeerklaeringId"
                                )
                            } else {
                                bucketService.deleteLegeerklaring(msgId)
                                database.slettLegeerklaering(legeerklaeringId)
                                log.info("Slettet legeerklæring med id $legeerklaeringId")
                            }
                        } else {
                            val legeerklaeringKafkaMessage: LegeerklaeringKafkaMessage =
                                objectMapper.readValue(consumerRecord.value())
                            val receivedLegeerklaering =
                                bucketService.getLegeerklaring(
                                    legeerklaeringKafkaMessage.legeerklaeringObjectId
                                )
                            val legeerklaeringSak =
                                LegeerklaeringSak(
                                    receivedLegeerklaering,
                                    legeerklaeringKafkaMessage.validationResult,
                                    legeerklaeringKafkaMessage.vedlegg,
                                )
                            handleLegeerklaringSak(legeerklaeringSak)
                        }
                    }
                }
            } catch (e: TrackableException) {
                log.error(
                    "Applikasjonen restarter {}",
                    StructuredArguments.fields(e.loggingMeta),
                    e.cause,
                )
            } catch (e: Exception) {
                log.error(
                    "Applikasjonen restarter",
                    e.cause,
                )
            } finally {
                applicationState.ready = false
                applicationState.alive = false
            }
        }
    }

    private suspend fun handleLegeerklaringSak(
        legeerklaeringSak: LegeerklaeringSak,
    ) {
        val loggingMeta =
            LoggingMeta(
                mottakId = legeerklaeringSak.receivedLegeerklaering.navLogId,
                orgNr = legeerklaeringSak.receivedLegeerklaering.legekontorOrgNr,
                msgId = legeerklaeringSak.receivedLegeerklaering.msgId,
                legeerklaeringId = legeerklaeringSak.receivedLegeerklaering.legeerklaering.id,
            )
        wrapExceptions(loggingMeta) {
            log.info("Mottok ein legeerklæring, {}", StructuredArguments.fields(loggingMeta))
            INCOMING_MESSAGE_COUNTER.inc()

            if (
                database.erLegeerklaeringsopplysningerLagret(
                    legeerklaeringSak.receivedLegeerklaering.legeerklaering.id,
                )
            ) {
                log.warn(
                    "Legeerklæring med legeerklæringsid {}, er allerede lagret i databasen, {}",
                    legeerklaeringSak.receivedLegeerklaering.legeerklaering.id,
                    StructuredArguments.fields(loggingMeta),
                )
            } else {
                database.lagreMottattLegeerklearing(legeerklaeringSak)
                log.info(
                    "Legeerklæring lagret i databasen, for {}",
                    StructuredArguments.fields(loggingMeta),
                )
                MESSAGE_STORED_IN_DB_COUNTER.inc()
            }
        }
    }
}
