package no.nav.syfo.utils

data class LoggingMeta(
    val mottakId: String,
    val orgNr: String?,
    val msgId: String,
    val legeerklaeringId: String,
)

class TrackableException(override val cause: Throwable, val loggingMeta: LoggingMeta) :
    RuntimeException()

suspend fun <O> wrapExceptions(loggingMeta: LoggingMeta, block: suspend () -> O): O {
    try {
        return block()
    } catch (e: Exception) {
        throw TrackableException(e, loggingMeta)
    }
}
