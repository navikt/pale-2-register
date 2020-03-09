package no.nav.syfo

import no.nav.syfo.kafka.KafkaConfig
import no.nav.syfo.kafka.KafkaCredentials

data class Environment(
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "pale-2-register"),
    val serviceuserUsernamePath: String = getEnvVar("SERVICE_USER_USERNAME"),
    val serviceuserPasswordPath: String = getEnvVar("SERVICE_USER_PASSWORD"),
    val syfosmmanuellbackendDBURL: String = getEnvVar("PALE_2_REGISTER_DB_URL"),
    val mountPathVault: String = getEnvVar("MOUNT_PATH_VAULT"),
    val databaseName: String = getEnvVar("DATABASE_NAME", "pale-2-register"),
    override val kafkaBootstrapServers: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),
    val pale2OkTopic: String = getEnvVar("KAFKA_PALE_2_OK_TOPIC", "privat-syfo-pale2-ok-v1"),
    val pale2AvvistTopic: String = getEnvVar("KAFKA_PALE_2_AVVIST_TOPIC", "privat-syfo-pale2-avvist-v1")
) : KafkaConfig

data class VaultSecrets(
    val serviceuserUsername: String,
    val serviceuserPassword: String
) : KafkaCredentials {
    override val kafkaUsername: String = serviceuserUsername
    override val kafkaPassword: String = serviceuserPassword
}

fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")