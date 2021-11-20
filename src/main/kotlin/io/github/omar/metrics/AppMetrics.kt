package io.github.omar.metrics

import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Counter
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import java.util.concurrent.atomic.AtomicInteger


val APP_METRICS_REGISTRY =
    PrometheusMeterRegistry(PrometheusConfig.DEFAULT, CollectorRegistry.defaultRegistry, Clock.SYSTEM)


val SUCCESSFUL_MESSAGES_SENT: Counter = Counter
    .builder("telegram_successful_message")
    .register(APP_METRICS_REGISTRY)

val UNSUCCESSFUL_MESSAGES_SENT: Counter = Counter
    .builder("telegram_unsuccessful_message")
    .register(APP_METRICS_REGISTRY)

val EVENT_SCHEDULER_MISFIRE: Counter = Counter
    .builder("event_scheduler_misfire")
    .register(APP_METRICS_REGISTRY)

fun successfulMessage() {
    SUCCESSFUL_MESSAGES_SENT.increment()
}

fun unsuccessfulMessage() {
    UNSUCCESSFUL_MESSAGES_SENT.increment()
}

fun scheduledTaskQueue(size: Int) {
    val atomicSize = AtomicInteger(size)
    APP_METRICS_REGISTRY.gauge("event_scheduler_task_queue", atomicSize)
}

fun failedScheduledTask() {
    EVENT_SCHEDULER_MISFIRE.increment()
}