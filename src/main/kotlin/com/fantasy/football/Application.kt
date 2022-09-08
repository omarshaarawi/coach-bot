package com.fantasy.football

import com.fantasy.football.config.DatabaseConfig
import com.fantasy.football.config.TelegramConfig
import com.fantasy.football.plugins.configureMonitoring
import com.fantasy.football.plugins.configureRouting
import com.fantasy.football.service.EventNotificationScheduler
import com.fantasy.football.service.TelegramService
import com.fantasy.football.service.YahooApiService
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import config.YahooConfig
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KotlinLogging
import service.YahooClient

private val LOGGER = KotlinLogging.logger { }

fun main() {
    lateinit var bot: Bot

    data class Test(val telegram: TelegramConfig, val yahoo: YahooConfig, val db: DatabaseConfig)

    val config = ConfigLoaderBuilder.default()
        .addResourceSource("/application.toml")
        .build()
        .loadConfigOrThrow<Test>()

    LOGGER.info { config.toString() }

    val yahoo = YahooApiService(YahooClient())
    val telegramService by lazy { TelegramService(bot, config.telegram) }

    bot = bot {
        token = config.telegram.token

        dispatch {
            command("scores") { telegramService.sendMessage(yahoo.getScoreBoard()) }
            command("proj") { telegramService.sendMessage(yahoo.getScoreBoard(projections = true)) }
            command("final") { telegramService.sendMessage(yahoo.getScoreBoard(final = true)) }
            command("matchups") { telegramService.sendMessage(yahoo.getMatchups()) }
            command("standings") { telegramService.sendMessage(yahoo.getStandings()) }
            command("waiver") { telegramService.sendMessage(yahoo.getTransactions()) }
        }
    }
    val scheduledMessages = mapOf(
        /** TODO :
         *  Close Scores - Mon - 18:30 east coast time
         *  Power rankings - Tue - 18:30 local time
         *  Players to Monitor report - Sun - 7:30 local time
         *  (Players in starting lineup that are Questionable, Doubtful, or Out)
         */

        { telegramService.sendMessage(yahoo.getTransactions()) } to Pair("every wed 08:00", "Waiver Report"),
        { telegramService.sendMessage(yahoo.getStandings()) } to Pair("every wed 08:00", "Current Standings"),
        { telegramService.sendMessage(yahoo.getMatchups()) } to Pair("every thu 18:30", "Matchups"),
        { telegramService.sendMessage(yahoo.getScoreBoard(final = true)) } to Pair(
            "every tue 08:00",
            "Final Score"
        ),
        { telegramService.sendMessage(yahoo.getScoreBoard()) } to Pair(
            "every fri,mon 08:00",
            "Score Update AM"
        ),
        { telegramService.sendMessage(yahoo.getScoreBoard()) } to Pair("every sun 15:00", "Score Update PM"),
        { telegramService.sendMessage(yahoo.getScoreBoard()) } to Pair("every sun 19:00", "Score Update PM Sunday"),
        { telegramService.sendMessage(yahoo.getScoreBoard(projections = true)) } to Pair(
            "every wed 08:00",
            "Score Projections"
        )
    )

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureMonitoring()
        configureRouting()
        bot.startPolling()

        scheduledMessages.forEach { (action, schedule) ->
            EventNotificationScheduler(config.telegram.timezone).scheduleNextExecution(action, schedule)
        }
    }.start(wait = true)
}
