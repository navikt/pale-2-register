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
                .withInitScript("db/testdb-init.sql")

        init {
            postgres.start()
        val env = EnvironmentVariables(
        databaseUsername = postgres.username,
        databasePassword = postgres.password,
        dbHost = postgres.host,
        dbPort = postgres.firstMappedPort.toString(),
        dbName = postgres.databaseName,
        legeerklaeringBucketName = "test-bucket",
        cluster = "test",
        applicationPort = 0,
        applicationName = "test",
    )
            
     database = Database(env)
     
        }
    }
}

fun Connection.dropData() {
    use { connection ->
        connection.prepareStatement("DELETE FROM LEGEERKLAERINGOPPLYSNINGER").executeUpdate()
        connection.commit()
    }
}
