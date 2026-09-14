import com.diffplug.gradle.spotless.SpotlessTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

group = "no.nav.syfo"
version = "1.0.0"

val javaVersion = JvmTarget.JVM_25

val ktorVersion = "3.5.2"
val logbackVersion = "1.6.3"
val logstashencoderVersion = "9.0"
val prometheusVersion = "0.16.0"
val junitjupiterVersion = "6.1.3"
val jacksonVersion = "3.2.2"
val postgresVersion = "42.7.13"
val flywayVersion = "13.5.0"
val hikariVersion = "7.1.0"
val testcontainerVersion = "2.0.5"
val mockkVersion = "1.14.11"
val googlecloudstorageVersion = "2.73.0"
val ktfmtVersion = "0.56"
val kafkaVersion = "4.3.1"

plugins {
    id("application")
    kotlin("jvm") version "2.4.20"
    id("com.diffplug.spotless") version "8.10.2"
}

application {
    mainClass.set("no.nav.syfo.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}
dependencies {

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    compileOnly("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashencoderVersion")

    implementation("tools.jackson.module:jackson-module-kotlin:${jacksonVersion}")

    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")

    implementation("com.google.cloud:google-cloud-storage:$googlecloudstorageVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitjupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitjupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitjupiterVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.testcontainers:testcontainers-postgresql:$testcontainerVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}

kotlin {
    compilerOptions {
        jvmTarget = javaVersion
    }
}

tasks {

    withType<Test> {
        useJUnitPlatform {}
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    withType<SpotlessTask> {
        spotless{
            kotlin { ktfmt(ktfmtVersion).kotlinlangStyle() }
            check {
                dependsOn("spotlessApply")
            }
        }
    }
}
