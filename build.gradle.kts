group = "no.nav.syfo"
version = "1.0.0"

val ktorVersion="2.3.9"
val logbackVersion="1.5.3"
val logstashencoderVersion="7.4"
val prometheusVersion="0.16.0"
val junitjupiterVersion="5.10.2"
val pale2commonVersion="2.0.4"
val jacksonVersion="2.16.1"
val postgresVersion="42.7.2"
val flywayVersion="10.9.0"
val hikariVersion="5.1.0"
val testcontainerVersion="1.19.7"
val mockkVersion="1.13.10"
val kotlinVersion="1.9.22"
val googlecloudstorageVersion="2.35.0"
val ktfmtVersion="0.44"
val jvmVersion= "17"
val snappyJavaVersion = "1.1.10.5"

plugins {
    id("application")
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
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
