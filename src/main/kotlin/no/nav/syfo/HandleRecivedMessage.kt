package no.nav.syfo

import io.ktor.util.KtorExperimentalAPI
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.model.LegeerklaeringSak
import no.nav.syfo.utils.LoggingMeta
import no.nav.syfo.utils.wrapExceptions

@KtorExperimentalAPI
suspend fun handleRecivedMessage(
    legeerklaeringSak: LegeerklaeringSak,
    loggingMeta: LoggingMeta
) {
    wrapExceptions(loggingMeta) {
        log.info("Mottok ein legereklearing, {}", fields(loggingMeta))
    }
}
