package no.nav.syfo.application

import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import no.nav.syfo.Environment
import no.nav.syfo.application.api.registerNaisApi
import no.nav.syfo.metrics.monitorHttpRequests

fun createApplicationEngine(
    env: Environment,
    applicationState: ApplicationState,
): ApplicationEngine =
    embeddedServer(Netty, env.applicationPort) {
        routing {
            registerNaisApi(applicationState)
        }
        intercept(ApplicationCallPipeline.Monitoring, monitorHttpRequests())
    }
