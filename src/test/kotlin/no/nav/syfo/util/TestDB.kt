package no.nav.syfo.util

import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.Environment
import no.nav.syfo.db.Database
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.VaultCredentialService
import no.nav.syfo.db.VaultCredentials
import no.nav.syfo.log
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.Connection

class PsqlContainer : PostgreSQLContainer<PsqlContainer>("postgres:12")
class TestDB private constructor() {
    companion object {
        var database: DatabaseInterface
        val vaultCredentialService = mockk<VaultCredentialService>()
        init {
            val psqlContainer: PsqlContainer = PsqlContainer()
                .withExposedPorts(5432)
                .withUsername("username")
                .withPassword("password")
                .withDatabaseName("database")
                .withInitScript("db/testdb-init.sql")
            psqlContainer.start()
            val mockEnv = mockk<Environment>(relaxed = true)
            every { mockEnv.mountPathVault } returns ""
            every { mockEnv.databaseName } returns "database"
            every { mockEnv.pale2registerDBURL } returns psqlContainer.jdbcUrl
            every { vaultCredentialService.renewCredentialsTaskData = any() } returns Unit
            every { vaultCredentialService.getNewCredentials(any(), any(), any()) } returns VaultCredentials(
                "1",
                "username",
                "password"
            )
            database = Database(mockEnv, vaultCredentialService)
            try {
                database = Database(mockEnv, vaultCredentialService)
            } catch (ex: Exception) {
                log.error("error", ex)
                database = Database(mockEnv, vaultCredentialService)
            }
        }
    }
}

fun Connection.dropData() {
    use { connection ->
        connection.prepareStatement("DELETE FROM LEGEERKLAERINGOPPLYSNINGER").executeUpdate()
        connection.commit()
    }
}
