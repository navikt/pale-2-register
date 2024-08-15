group = "no.nav.syfo"
version = "1.0.0"

val ktorVersion = "2.3.12"
val logbackVersion = "1.5.6"
val logstashencoderVersion = "8.0"
val prometheusVersion = "0.16.0"
val junitjupiterVersion = "5.11.0"
val pale2commonVersion = "2.0.9"
val jacksonVersion = "2.17.2"
val postgresVersion = "42.7.3"
val flywayVersion = "10.17.1"
val hikariVersion = "5.1.0"
val testcontainerVersion = "1.20.1"
val mockkVersion = "1.13.12"
val kotlinVersion = "2.0.10"
val googlecloudstorageVersion = "2.41.0"
val ktfmtVersion = "0.44"
val jvmVersion = "17"
val snappyJavaVersion = "1.1.10.6"
val commonsCompressVersion = "1.27.0"
val nettyCodecHttp = "4.1.112.Final"

plugins {
    id("application")
    kotlin("jvm") version "2.0.10"
    id("com.gradleup.shadow") version "8.3.0"
    id("com.diffplug.spotless") version "6.25.0"
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
    constraints {
        implementation("io.netty:netty-codec-http:$nettyCodecHttp") {
            because("override transient from io.ktor:ktor-server-netty")
        }
    }

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

    implementation("no.nav.syfo:pale-2-common-models:$pale2commonVersion")
    implementation("no.nav.syfo:pale-2-common-kafka:$pale2commonVersion")
    constraints {
        implementation("org.xerial.snappy:snappy-java:$snappyJavaVersion") {
            because("override transient from org.apache.kafka:kafka_2.12")
        }
    }

    implementation("com.google.cloud:google-cloud-storage:$googlecloudstorageVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitjupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitjupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitjupiterVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.testcontainers:postgresql:$testcontainerVersion")
    constraints {
        implementation("org.apache.commons:commons-compress:$commonsCompressVersion") {
            because("Due to vulnerabilities, see CVE-2024-26308")
        }
    }
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
}

tasks {
    shadowJar {
        mergeServiceFiles {
            setPath("META-INF/services/org.flywaydb.core.extensibility.Plugin")
        }
        archiveBaseName.set("app")
        archiveClassifier.set("")
        isZip64 = true
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to "no.nav.syfo.ApplicationKt",
                ),
            )
        }
    }

    test {
        useJUnitPlatform {}
        testLogging {
            events("skipped", "failed")
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    spotless {
        kotlin { ktfmt(ktfmtVersion).kotlinlangStyle() }
        check {
            dependsOn("spotlessApply")
        }
    }
}
