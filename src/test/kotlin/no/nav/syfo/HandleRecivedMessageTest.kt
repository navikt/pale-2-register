package no.nav.syfo

import no.nav.syfo.model.LegeerklaeringSak
import no.nav.syfo.persistering.db.erLegeerklaeringsopplysningerLagret
import no.nav.syfo.persistering.db.hentMsgId
import no.nav.syfo.persistering.db.lagreMottattLegeerklearing
import no.nav.syfo.persistering.db.slettLegeerklaering
import no.nav.syfo.util.TestDB
import no.nav.syfo.util.dropData
import no.nav.syfo.util.hentLegeerklearing
import no.nav.syfo.util.receivedLegeerklaering
import no.nav.syfo.util.validationResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class HandleRecivedMessageTest {

    private val database = TestDB.database

    val legeerklaeringSak = LegeerklaeringSak(receivedLegeerklaering, validationResult, emptyList())

    @BeforeEach
    fun setup() {
        database.connection.dropData()
    }

    @Test
    internal fun `Check that legeerklaering is already stored in db`() {
        database.lagreMottattLegeerklearing(legeerklaeringSak)

        assertEquals(
            true,
            database.erLegeerklaeringsopplysningerLagret(legeerklaeringSak.receivedLegeerklaering.legeerklaering.id),
        )
    }

    @Test
    internal fun `Check that legeerklaering is not already stored in db`() {
        database.lagreMottattLegeerklearing(legeerklaeringSak)

        assertEquals(false, database.erLegeerklaeringsopplysningerLagret("23"))
    }

    @Test
    internal fun `Check legeerklaering is stored in db`() {
        database.lagreMottattLegeerklearing(legeerklaeringSak)

        val legeerklaeringOpplysninger = database.hentLegeerklearing(legeerklaeringSak.receivedLegeerklaering.legeerklaering.id)

        assertEquals(legeerklaeringSak.receivedLegeerklaering.personNrPasient, legeerklaeringOpplysninger.first().pasient_fnr)
    }

    @Test
    internal fun `Legeerklaering slettes fra db`() {
        database.lagreMottattLegeerklearing(legeerklaeringSak)
        assertEquals(
            true,
            database.erLegeerklaeringsopplysningerLagret(
                legeerklaeringSak.receivedLegeerklaering.legeerklaering.id,
            ),
        )

        assertEquals(receivedLegeerklaering.msgId, database.hentMsgId(legeerklaeringSak.receivedLegeerklaering.legeerklaering.id))

        database.slettLegeerklaering(legeerklaeringSak.receivedLegeerklaering.legeerklaering.id)

        assertEquals(
            false,
            database.erLegeerklaeringsopplysningerLagret(
                legeerklaeringSak.receivedLegeerklaering.legeerklaering.id,
            ),
        )
    }
}
