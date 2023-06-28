import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.syfo"
version = "1.0.0"

val ktor_version: String by project
val logback_version: String by project
val logstashEncoder_version: String by project
val prometheus_version: String by project
val junitJupiter_version: String by project
val pale2Common_version: String by project
val jackson_version: String by project
val postgres_version: String by project
val flyway_version: String by project
val hikari_version: String by project
val testContainer_version: String by project
val mockk_version: String by project
val kotlin_version: String by project
val googleCloudStorage_version: String by project
val ktfmt_version: String by project

application {
    mainClass.set("no.nav.syfo.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

plugins {
    java
    kotlin("jvm") version "1.8.22"
    id("io.ktor.plugin") version "2.3.2"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.diffplug.spotless") version "6.19.0"
    id("org.cyclonedx.bom") version "1.7.4"
}

val githubUser: String by project
val githubPassword: String by project

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/navikt/pale-2-common")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
}
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")

    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")

    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("com.zaxxer:HikariCP:$hikari_version")
    implementation("org.flywaydb:flyway-core:$flyway_version")

    implementation("io.prometheus:simpleclient_hotspot:$prometheus_version")
    implementation("io.prometheus:simpleclient_common:$prometheus_version")

    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoder_version")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")

    implementation("no.nav.syfo:pale-2-common-models:$pale2Common_version")
    implementation("no.nav.syfo:pale-2-common-kafka:$pale2Common_version")

    implementation("com.google.cloud:google-cloud-storage:$googleCloudStorage_version")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiter_version")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiter_version")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitJupiter_version")

    testImplementation("org.testcontainers:postgresql:$testContainer_version")
    testImplementation("io.mockk:mockk:$mockk_version")
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version") {
        exclude(group = "org.eclipse.jetty")
    }
}

tasks {
    withType<Jar> {
        manifest.attributes["Main-Class"] = "no.nav.syfo.ApplicationKt"
    }
    create("printVersion") {
        doLast {
            println(project.version)
        }
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    withType<ShadowJar> {
        transform(ServiceFileTransformer::class.java) {
            setPath("META-INF/cxf")
            include("bus-extensions.txt")
        }
    }

    withType<Test> {
        useJUnitPlatform {}
        testLogging {
            events("skipped", "failed")
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    spotless {
        kotlin { ktfmt(ktfmt_version).kotlinlangStyle() }
        check {
            dependsOn("spotlessApply")
        }
    }
}
