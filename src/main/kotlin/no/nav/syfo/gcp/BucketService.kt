package no.nav.syfo.gcp

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.cloud.storage.Storage
import no.nav.syfo.log
import no.nav.syfo.model.ReceivedLegeerklaering
import no.nav.syfo.objectMapper

class BucketService(
    private val name: String,
    private val storage: Storage
) {

    fun getLegeerklaring(objectId: String): ReceivedLegeerklaering {
        val blob = storage.get(name, objectId)
        if (blob == null) {
            log.error("Legeerklæring er null $objectId")
            throw RuntimeException("Legeerklæring er null $objectId")
        }

        val content = blob.getContent()
        log.info("Har hentet legeerklæring for $objectId")
        return objectMapper.readValue(content)
    }
}
