package com.fantasy.football.service

import com.github.shyiko.skedule.Schedule
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import mu.KotlinLogging

class EventNotificationScheduler(private val timezone: String) {

    private val executor = ScheduledThreadPoolExecutor(10)

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
                scheduleNextExecution(action, stringSchedule)
            }, nextExecution.toEpochSecond() - now.toEpochSecond(), TimeUnit.SECONDS)
                    LOGGER.info { "Schedule ${stringSchedule.second} notification for $nextExecution" }
                } catch (ex: Exception) {
                    LOGGER.error(ex) { "Could not schedule next notification!" }
                }
            }

            companion object {
                private val LOGGER = KotlinLogging.logger { }
            }
        }
        