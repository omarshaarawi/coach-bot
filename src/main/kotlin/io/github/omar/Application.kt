package io.github.omar

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import io.github.omar.config.Config
import io.github.omar.model.ESPNConfig
import io.github.omar.model.TelegramConfig
import io.github.omar.plugins.configureAdministration
import io.github.omar.plugins.configureMonitoring
import io.github.omar.plugins.configureRouting
import io.github.omar.service.EspnService
import io.github.omar.service.EventNotificationScheduler
import io.github.omar.service.TelegramService
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KotlinLogging

private val LOGGER = KotlinLogging.logger {}
fun main() {

    lateinit var bot: Bot

    val config = Config.loadConfig()

    val espnConfig = ESPNConfig(
        espnS2 = config[Config.Ktor.ESPN.espnS2],
        swid = config[Config.Ktor.ESPN.swid],
        leagueId = config[Config.Ktor.ESPN.leagueID]
    )

    val telegramConfig = TelegramConfig(
        token = config[Config.Ktor.Telegram.token],
        chatID = config[Config.Ktor.Telegram.chatID],
        timezone = config[Config.Ktor.Telegram.timezone]
    )


    val espnService = EspnService(espnConfig)
    val telegramService by lazy { TelegramService(bot, telegramConfig) }

    LOGGER.info { "Using v0.5.0 of espn-ff-kotlin" }

    bot = bot {
        token = telegramConfig.token

        dispatch {
            command("whohas") {
                telegramService.sendMessage(espnService.whoHas(message.text!!.replace("/whohas ",
                    "")))
            }
            command("power") { telegramService.sendMessage(espnService.getPowerRankings()) }
            command("matchups") { telegramService.sendMessage(espnService.getMatchups()) }
            command("final") { telegramService.sendMessage(espnService.scoreboard(true)) }
            command("scores") { telegramService.sendMessage(espnService.scoreboard()) }
            command("projections") { telegramService.sendMessage(espnService.getProjectedScoreboard()) }
            command("waiver") { telegramService.sendMessage(espnService.getWaiverReport()) }
            command("monitor") { telegramService.sendMessage(espnService.getMonitor()) }
            command("refresh") { espnService.refresh() }
        }
    }

    val map = mapOf(
        { telegramService.sendMessage(espnService.getPowerRankings()) } to Pair("every tue 18:30", "Power Rankings"),
        { telegramService.sendMessage(espnService.getMatchups()) } to Pair("every thu 18:00", "Matchups"),
        { telegramService.sendMessage(espnService.getCloseScores()) } to Pair("every mon 17:30", "Close Scores"),
        { telegramService.sendMessage(espnService.scoreboard(true)) } to Pair("every tue 08:00", "Final Score"),
        { telegramService.sendMessage(espnService.scoreboard()) } to Pair("every fri,mon 07:30", "Score Update AM"),
        { telegramService.sendMessage(espnService.scoreboard()) } to Pair("every sun 15:30", "Score Update PM"),
        { telegramService.sendMessage(espnService.scoreboard()) } to Pair("every sun 19:00", "Score Update PM Sunday"),
        { telegramService.sendMessage(espnService.getProjectedScoreboard()) } to Pair("every wed 18:00",
            "Score Projections"),
        { telegramService.sendMessage(espnService.getWaiverReport()) } to Pair("every wed 08:00", "Waiver Report"),
        { telegramService.sendMessage(espnService.getMonitor()) } to Pair("every sun 08:00", "Player Monitor"),
    )
    
    embeddedServer(Netty, config[Config.Ktor.Deployment.port], config[Config.Ktor.Deployment.host]) {
        LOGGER.info { "Starting bot polling" }
        bot.startPolling()
        LOGGER.info { "Scheduling ${map.size} Jobs" }

        map.forEach { (action, schedule) ->
            EventNotificationScheduler(telegramConfig.timezone).scheduleNextExecution(action, schedule)
        }

        configureAdministration()
        configureRouting()
        configureMonitoring()

    }.start(wait = true)
}