package no.nav.syfo

data class Environment(
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "pale-2-register"),
    val pale2registerDBURL: String = getEnvVar("PALE_2_REGISTER_DB_URL"),
    val mountPathVault: String = getEnvVar("MOUNT_PATH_VAULT"),
    val databaseName: String = getEnvVar("DATABASE_NAME", "pale-2-register"),
    val pale2OkTopic: String = getEnvVar("KAFKA_PALE_2_OK_TOPIC", "privat-syfo-pale2-ok-v1"),
    val pale2AvvistTopic: String = getEnvVar("KAFKA_PALE_2_AVVIST_TOPIC", "privat-syfo-pale2-avvist-v1"),
    val developmentMode: Boolean = getEnvVar("DEVELOPMENT_MODE", "false").toBoolean(),
    val legeerklaeringBucketName: String = getEnvVar("PALE_BUCKET_NAME"),
    val legeerklaringTopic: String = "teamsykmelding.legeerklaering"
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
