package no.nav.syfo.persistering.db

import java.sql.Connection
import java.sql.Timestamp
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.model.Legeerklaering
import no.nav.syfo.model.LegeerklaeringSak
import no.nav.syfo.model.ReceivedLegeerklaering
import no.nav.syfo.model.ValidationResult
import no.nav.syfo.objectMapper
import org.postgresql.util.PGobject

fun DatabaseInterface.lagreMottattLegeerklearing(legeerklaeringSak: LegeerklaeringSak) {
    connection.use { connection ->
        connection.opprettLegeerklaeringOpplysninger(legeerklaeringSak.receivedLegeerklaering)
        connection.opprettLegeerklaeringsdokument(
            legeerklaeringSak.receivedLegeerklaering.legeerklaering
        )
        connection.opprettBehandlingsutfall(
            legeerklaeringSak.validationResult,
            legeerklaeringSak.receivedLegeerklaering.legeerklaering.id,
        )
        connection.commit()
    }
}

private fun Connection.opprettLegeerklaeringOpplysninger(
    receivedLegeerklaering: ReceivedLegeerklaering
) {
    this.prepareStatement(
            """
            INSERT INTO LEGEERKLAERINGOPPLYSNINGER(
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
                )
            VALUES  (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
        )
        .use {
            it.setString(1, receivedLegeerklaering.legeerklaering.id)
            it.setString(2, receivedLegeerklaering.legeerklaering.pasient.fnr)
            it.setString(3, receivedLegeerklaering.pasientAktoerId)
            it.setString(4, receivedLegeerklaering.personNrLege)
            it.setString(5, receivedLegeerklaering.legeAktoerId)
            it.setString(6, receivedLegeerklaering.navLogId)
            it.setString(7, receivedLegeerklaering.msgId)
            it.setString(8, receivedLegeerklaering.legekontorOrgNr)
            it.setString(9, receivedLegeerklaering.legekontorHerId)
            it.setString(10, receivedLegeerklaering.legekontorReshId)
            it.setTimestamp(11, Timestamp.valueOf(receivedLegeerklaering.mottattDato))
            it.setString(12, receivedLegeerklaering.tssid)
            it.executeUpdate()
        }
}

private fun Connection.opprettLegeerklaeringsdokument(legeerklaering: Legeerklaering) {
    this.prepareStatement(
            """
            INSERT INTO LEGEERKLAERINGSDOKUMENT(id, legerklearing) VALUES  (?, ?)
            """,
        )
        .use {
            it.setString(1, legeerklaering.id)
            it.setObject(2, legeerklaering.toPGObject())
            it.executeUpdate()
        }
}

private fun Connection.opprettBehandlingsutfall(
    validationResult: ValidationResult,
    legeerklaeringid: String
) {
    this.prepareStatement(
            """
                    INSERT INTO BEHANDLINGSUTFALL(id, behandlingsutfall) VALUES (?, ?)
                """,
        )
        .use {
            it.setString(1, legeerklaeringid)
            it.setObject(2, validationResult.toPGObject())
            it.executeUpdate()
        }
}

fun DatabaseInterface.erLegeerklaeringsopplysningerLagret(legeerklaeringid: String) =
    connection.use { connection ->
        connection
            .prepareStatement(
                """
                SELECT true
                FROM LEGEERKLAERINGOPPLYSNINGER
                WHERE id=?;
                """,
            )
            .use {
                it.setString(1, legeerklaeringid)
                it.executeQuery().next()
            }
    }

fun DatabaseInterface.slettLegeerklaering(legeerklaeringId: String) {
    connection.use { connection ->
        connection.slettLegeerklaering(legeerklaeringId)
        connection.commit()
    }
}

private fun Connection.slettLegeerklaering(legeerklaeringid: String) {
    this.prepareStatement(
            """
                    DELETE FROM LEGEERKLAERINGOPPLYSNINGER WHERE id=?;
                """,
        )
        .use {
            it.setString(1, legeerklaeringid)
            it.executeUpdate()
        }
}

fun DatabaseInterface.hentMsgId(legeerklaeringId: String): String? {
    connection.use { connection ->
        connection
            .prepareStatement(
                """
                 SELECT msg_id 
                 FROM LEGEERKLAERINGOPPLYSNINGER 
                 WHERE id=?;
                """,
            )
            .use { preparedStatement ->
                preparedStatement.setString(1, legeerklaeringId)
                preparedStatement.executeQuery().use { resultSet ->
                    when (resultSet.next()) {
                        true -> return resultSet.getString("msg_id")
                        else -> return null
                    }
                }
            }
    }
}

fun Legeerklaering.toPGObject() =
    PGobject().also {
        it.type = "json"
        it.value = objectMapper.writeValueAsString(this)
    }

fun ValidationResult.toPGObject() =
    PGobject().also {
        it.type = "json"
        it.value = objectMapper.writeValueAsString(this)
    }
