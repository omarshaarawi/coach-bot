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
import service.YahooClient

fun main() {
    lateinit var bot: Bot

    data class Test(val telegram: TelegramConfig, val yahoo: YahooConfig, val db: DatabaseConfig)

    val config = ConfigLoaderBuilder.default()
        .addResourceSource("/application.toml")
        .build()
        .loadConfigOrThrow<Test>()

    val yahoo = YahooApiService(YahooClient())
    val telegramService by lazy { TelegramService(bot, config.telegram) }

    bot = bot {
        token = config.telegram.token

        dispatch {
            command("scores") { telegramService.sendMessage(yahoo.getScoreBoard()) }
            command("proj") { telegramService.sendMessage(yahoo.getScoreBoard(projections = true)) }
            command("final") { telegramService.sendMessage(yahoo.getScoreBoard(final = true)) }
            command("matchups") { telegramService.sendMessage(yahoo.getMatchups()) }
        }
    }
    val scheduledMessages = mapOf(
        /** TODO :
         *  power rankings - every tue 17:30
         *  close scores - every tue 17:30
         *  waiver report - every wed 08:00
         *  player monitor - every sun 08:00
         */
        { telegramService.sendMessage(yahoo.getMatchups()) } to Pair("every thu 18:30", "Matchups"),
        { telegramService.sendMessage(yahoo.getScoreBoard(final = true)) } to Pair(
            "every tue 08:00",
            "Final Score"
        ),
        { telegramService.sendMessage(yahoo.getScoreBoard()) } to Pair(
            "every fri,mon,tue 08:00",
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
