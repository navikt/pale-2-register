package no.nav.syfo.persistering

import io.ktor.util.KtorExperimentalAPI
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.db.Database
import no.nav.syfo.log
import no.nav.syfo.metrics.INCOMING_MESSAGE_COUNTER
import no.nav.syfo.metrics.MESSAGE_STORED_IN_DB_COUNTER
import no.nav.syfo.model.LegeerklaeringSak
import no.nav.syfo.persistering.db.erLegeerklaeringsopplysningerLagret
import no.nav.syfo.persistering.db.lagreMottattSykmelding
import no.nav.syfo.utils.LoggingMeta
import no.nav.syfo.utils.wrapExceptions

@KtorExperimentalAPI
suspend fun handleRecivedMessage(
    legeerklaeringSak: LegeerklaeringSak,
    loggingMeta: LoggingMeta,
    database: Database
) {
    wrapExceptions(loggingMeta) {
        log.info("Mottok ein legereklearing, {}", fields(loggingMeta))
        INCOMING_MESSAGE_COUNTER.inc()

        if (database.connection.erLegeerklaeringsopplysningerLagret(
                legeerklaeringSak.receivedLegeerklaering.legeerklaering.id)) {
            log.warn(
                "Legereklearing med legereklearingsid {}, er allerede lagret i databasen, {}",
                legeerklaeringSak.receivedLegeerklaering.legeerklaering.id, fields(loggingMeta)
            )
        } else {

            database.lagreMottattSykmelding(legeerklaeringSak)
            log.info(
                "Manuell oppgave lagret i databasen, for {}",
                fields(loggingMeta)
            )
            MESSAGE_STORED_IN_DB_COUNTER.inc()
        }
    }
}
