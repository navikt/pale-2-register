package no.nav.syfo

import no.nav.syfo.kafka.KafkaConfig
import no.nav.syfo.kafka.KafkaCredentials
import no.nav.syfo.utils.getFileAsString

data class Environment(
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "pale-2-register"),
    val pale2registerDBURL: String = getEnvVar("PALE_2_REGISTER_DB_URL"),
    val mountPathVault: String = getEnvVar("MOUNT_PATH_VAULT"),
    val databaseName: String = getEnvVar("DATABASE_NAME", "pale-2-register"),
    override val kafkaBootstrapServers: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),
    val pale2OkTopic: String = getEnvVar("KAFKA_PALE_2_OK_TOPIC", "privat-syfo-pale2-ok-v1"),
    val pale2AvvistTopic: String = getEnvVar("KAFKA_PALE_2_AVVIST_TOPIC", "privat-syfo-pale2-avvist-v1"),
    val developmentMode: Boolean = getEnvVar("DEVELOPMENT_MODE", "false").toBoolean()
) : KafkaConfig

data class VaultSecrets(
    val serviceuserUsername: String = getFileAsString("/secrets/serviceuser/username"),
    val serviceuserPassword: String = getFileAsString("/secrets/serviceuser/password")
) : KafkaCredentials {
    override val kafkaUsername: String = serviceuserUsername
    override val kafkaPassword: String = serviceuserPassword
}

fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
