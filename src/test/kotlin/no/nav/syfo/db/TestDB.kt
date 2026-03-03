package no.nav.syfo.db

import io.mockk.every
import io.mockk.mockk
import java.sql.Connection
import no.nav.syfo.EnvironmentVariables
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

class TestDB private constructor() {
    companion object {
        var database: DatabaseInterface
        var postgres =
            PostgreSQLContainer(DockerImageName.parse("postgres:14"))
                .withPassword("password")
                .withUsername("postgres")
                .withDatabaseName("postgres")

        init {
            postgres.start()
            val mockEnv = mockk<EnvironmentVariables>(relaxed = true)
            every { mockEnv.dbPort } returns postgres.firstMappedPort.toString()
            every { mockEnv.databaseUsername } returns "postgres"
            every { mockEnv.databasePassword } returns "password"
            every { mockEnv.dbName } returns "postgres"
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
