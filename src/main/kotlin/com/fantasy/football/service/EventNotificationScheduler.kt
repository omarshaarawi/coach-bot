package com.fantasy.football.service

import com.fantasy.football.exceptions.SchedulerException
import com.github.shyiko.skedule.Schedule
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import mu.KotlinLogging

class EventNotificationScheduler(private val timezone: String) {

    private val executor = ScheduledThreadPoolExecutor(CORE_POOL_SIZE)

    companion object {
        private val LOGGER = KotlinLogging.logger { }
        private const val CORE_POOL_SIZE = 10
    }

    init {
        executor.removeOnCancelPolicy = true
    }

    fun scheduleNextExecution(action: () -> Unit, stringSchedules: List<Pair<String, String>>?) {
        stringSchedules!!.forEach { schedule ->
            try {
                val parsedSchedule = Schedule.parse(schedule.first)
                val now = ZonedDateTime.now(ZoneId.of(timezone))
                val nextExecution = parsedSchedule.next(now).withZoneSameInstant(ZoneId.of("America/Chicago"))
                executor.schedule({
                    LOGGER.info { "Trigger notification event" }
                    action.invoke()
                    scheduleNextExecution(action, listOf(schedule))
                }, nextExecution.toEpochSecond() - now.toEpochSecond(), TimeUnit.SECONDS)
                LOGGER.info { "Schedule ${schedule.second} notification for $nextExecution" }
            } catch (ex: SchedulerException) {
                LOGGER.error(ex) { "Could not schedule next notification!" }
            }
        }
    }
}
