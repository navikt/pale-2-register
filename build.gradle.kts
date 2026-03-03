import com.diffplug.gradle.spotless.SpotlessTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

group = "no.nav.syfo"
version = "1.0.0"

val javaVersion = JvmTarget.JVM_25

val ktorVersion = "3.3.1"
val logbackVersion = "1.5.17"
val logstashencoderVersion = "8.0"
val prometheusVersion = "0.16.0"
val junitjupiterVersion = "5.12.0"
val jacksonVersion = "2.18.3"
val postgresVersion = "42.7.5"
val flywayVersion = "11.3.4"
val hikariVersion = "6.2.1"
val testcontainerVersion = "2.0.3"
val mockkVersion = "1.13.17"
val kotlinVersion = "2.3.10"
val googlecloudstorageVersion = "2.49.0"
val ktfmtVersion = "0.44"
val kafkaVersion = "3.9.0"

///Due to vulnerabilities
val commonsCompressVersion = "1.27.1"

plugins {
    id("application")
    kotlin("jvm") version "2.3.10"
    id("com.diffplug.spotless") version "8.2.1"
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

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

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")


    implementation("org.apache.kafka:kafka_2.12:$kafkaVersion")

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
