package no.nav.syfo.util

import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.toList
import java.sql.Connection
import java.sql.ResultSet
import java.time.LocalDateTime

fun DatabaseInterface.hentLegeerklearing(legeerklaeringId: String): List<LegeerklaeringOpplysninger> =
    connection.use { connection ->
        val legeerklaeringOpplysninger = connection.hentLegeerklaeringOpplysninger(legeerklaeringId)
        return legeerklaeringOpplysninger
    }

private fun Connection.hentLegeerklaeringOpplysninger(legeerklaeringId: String): List<LegeerklaeringOpplysninger> =
    this.prepareStatement(
        """
            SELECT 
                id,
                pasient_fnr,
                pasient_aktoer_id,
                lege_fnr,
                lege_aktoer_id,
                mottak_id,
                msg_id,
                legekontor_org_nr,
                legekontor_her_id,
                legekontor_resh_id,
                mottatt_tidspunkt,
                tss_id
            FROM LEGEERKLAERINGOPPLYSNINGER
            WHERE id = ?;
            """,
    ).use {
        it.setString(1, legeerklaeringId)
        it.executeQuery().toList { toLegeerklaeringOpplysninger() }
    }

fun ResultSet.toLegeerklaeringOpplysninger(): LegeerklaeringOpplysninger =
    LegeerklaeringOpplysninger(
        id = getString("id").trim(),
        pasient_fnr = getString("pasient_fnr").trim(),
        pasient_aktoer_id = getString("pasient_aktoer_id").trim(),
        lege_fnr = getString("lege_fnr").trim(),
        lege_aktoer_id = getString("lege_aktoer_id").trim(),
        mottak_id = getString("mottak_id").trim(),
        msg_id = getString("msg_id").trim(),
        legekontor_org_nr = getString("legekontor_org_nr").trim(),
        legekontor_her_id = getString("legekontor_her_id").trim(),
        legekontor_resh_id = getString("legekontor_resh_id").trim(),
        mottatt_tidspunkt = getTimestamp("mottatt_tidspunkt").toLocalDateTime(),
        tss_id = getString("tss_id").trim(),
    )

data class LegeerklaeringOpplysninger(
    val id: String,
    val pasient_fnr: String,
    val pasient_aktoer_id: String,
    val lege_fnr: String,
    val lege_aktoer_id: String,
    val mottak_id: String,
    val msg_id: String,
    val legekontor_org_nr: String?,
    val legekontor_her_id: String?,
    val legekontor_resh_id: String?,
    val mottatt_tidspunkt: LocalDateTime?,
    val tss_id: String?,
)
