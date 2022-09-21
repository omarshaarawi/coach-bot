package com.fantasy.football.service

import com.fantasy.football.exceptions.SchedulerException
import com.github.shyiko.skedule.Schedule
import mu.KotlinLogging
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class EventNotificationScheduler(private val timezone: String) {

    private val executor = ScheduledThreadPoolExecutor(CORE_POOL_SIZE)

    companion object {
        private val LOGGER = KotlinLogging.logger { }
        private const val CORE_POOL_SIZE = 10
    }

    init {
        executor.removeOnCancelPolicy = true
    }

    fun scheduleNextExecution(
        action: (String) -> Lazy<Unit>,
        stringSchedules: List<Pair<String, String>>?,
        chatId: String
    ) {
        stringSchedules!!.forEach { schedule ->
            try {
                val parsedSchedule = Schedule.parse(schedule.first)
                val now = ZonedDateTime.now(ZoneId.of(timezone))
                val nextExecution = parsedSchedule.next(now).withZoneSameInstant(ZoneId.of("America/Chicago"))
                executor.schedule({
                    LOGGER.info { "Trigger notification event" }
                    action.invoke(chatId).value
                    scheduleNextExecution(action, listOf(schedule), chatId)
                }, nextExecution.toEpochSecond() - now.toEpochSecond(), TimeUnit.SECONDS)
                LOGGER.info { "Schedule ${schedule.second} notification for $nextExecution" }
            } catch (ex: SchedulerException) {
                LOGGER.error(ex) { "Could not schedule next notification!" }
            }
        }
    }

    fun scheduleNextExecution(
        action: () -> Unit,
        schedule: Pair<String, String>,
    ) {
        try {
            val parsedSchedule = Schedule.parse(schedule.first)
            val now = ZonedDateTime.now(ZoneId.of(timezone))
            val nextExecution = parsedSchedule.next(now).withZoneSameInstant(ZoneId.of("America/Chicago"))
            executor.schedule({
                LOGGER.info { "Trigger notification event" }
                action.invoke()
                scheduleNextExecution(action, schedule)
            }, nextExecution.toEpochSecond() - now.toEpochSecond(), TimeUnit.SECONDS)
            LOGGER.info { "Schedule ${schedule.second} notification for $nextExecution" }
        } catch (ex: SchedulerException) {
            LOGGER.error(ex) { "Could not schedule next notification!" }
        }
    }
}
