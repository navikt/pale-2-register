package no.nav.syfo.metrics

import io.prometheus.client.Counter

const val METRICS_NS = "pale2register"

val MESSAGE_STORED_IN_DB_COUNTER: Counter =
    Counter.build()
        .namespace(METRICS_NS)
        .name("message_stored_in_db_count")
        .help("Counts the number of messages stored in db")
        .register()

val INCOMING_MESSAGE_COUNTER: Counter =
    Counter.build()
        .namespace(METRICS_NS)
        .name("incoming_message_count")
        .help("Counts the number of incoming messages")
        .register()
