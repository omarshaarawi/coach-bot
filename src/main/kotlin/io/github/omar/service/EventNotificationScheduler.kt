package io.github.omar.service

import com.github.shyiko.skedule.Schedule
import io.github.omar.metrics.failedScheduledTask
import io.github.omar.metrics.scheduledTaskQueue
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import mu.KotlinLogging


class EventNotificationScheduler(private val timezone: String) {

    private val executor = ScheduledThreadPoolExecutor(1)

    init {
        executor.removeOnCancelPolicy = true
    }

    fun scheduleNextExecution(action: () -> Unit, stringSchedule: Pair<String, String>) {
        try {
            val schedule = Schedule.parse(stringSchedule.first)
            val now = ZonedDateTime.now(ZoneId.of(timezone))
            val nextExecution = schedule.next(now).withZoneSameInstant(ZoneId.of("America/Chicago"))
            executor.schedule({
                LOGGER.info { "Trigger notification event" }
                action.invoke()
                scheduledTaskQueue(executor.queue.size)
                scheduleNextExecution(action, stringSchedule)
            }, nextExecution.toEpochSecond() - now.toEpochSecond(), TimeUnit.SECONDS)
            LOGGER.info { "Schedule ${stringSchedule.second} notification for $nextExecution" }
        } catch (ex: Exception) {
            LOGGER.error(ex) { "Could not schedule next notification!" }
            failedScheduledTask()
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}
