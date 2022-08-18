package no.nav.syfo

import no.nav.syfo.model.LegeerklaeringSak
import no.nav.syfo.persistering.db.erLegeerklaeringsopplysningerLagret
import no.nav.syfo.persistering.db.lagreMottattLegeerklearing
import no.nav.syfo.util.TestDB
import no.nav.syfo.util.dropData
import no.nav.syfo.util.hentLegeerklearing
import no.nav.syfo.util.receivedLegeerklaering
import no.nav.syfo.util.validationResult
import org.amshove.kluent.shouldBeEqualTo
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

        database.erLegeerklaeringsopplysningerLagret(
            legeerklaeringSak.receivedLegeerklaering.legeerklaering.id
        ) shouldBeEqualTo true
    }

    @Test
    internal fun `Check that legeerklaering is not already stored in db`() {
        database.lagreMottattLegeerklearing(legeerklaeringSak)

        database.erLegeerklaeringsopplysningerLagret("23") shouldBeEqualTo false
    }

    @Test
    internal fun `Check legeerklaering is stored in db`() {
        database.lagreMottattLegeerklearing(legeerklaeringSak)

        val legeerklaeringOpplysninger = database.hentLegeerklearing(legeerklaeringSak.receivedLegeerklaering.legeerklaering.id)

        legeerklaeringOpplysninger.first().pasient_fnr shouldBeEqualTo legeerklaeringSak.receivedLegeerklaering.personNrPasient
    }
}
