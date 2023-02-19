package no.nav.syfo.util

import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.Environment
import no.nav.syfo.db.Database
import no.nav.syfo.db.DatabaseInterface
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.Connection

class TestDB private constructor() {
    companion object {
        var database: DatabaseInterface

        init {
            val postgres = PostgreSQLContainer<Nothing>("postgres:14").apply {
                withCommand("postgres", "-c", "wal_level=logical")
                withUsername("username")
                withPassword("password")
                withDatabaseName("database")
                withInitScript("db/testdb-init.sql")
                start()
                println("Database: jdbc:postgresql://localhost:$firstMappedPort/test startet opp, credentials: test og test")
            }

            val mockEnv = mockk<Environment>(relaxed = true)
            every { mockEnv.databaseUsername } returns postgres.username
            every { mockEnv.databasePassword } returns postgres.password
            every { mockEnv.dbName } returns postgres.databaseName
            every { mockEnv.dbPort } returns postgres.firstMappedPort.toString()
            database = Database(mockEnv)
        }
    }
}

fun Connection.dropData() {
    use { connection ->
        connection.prepareStatement("DELETE FROM LEGEERKLAERINGOPPLYSNINGER").executeUpdate()
        connection.commit()
    }
}
