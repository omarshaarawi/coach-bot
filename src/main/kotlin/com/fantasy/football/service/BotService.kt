package com.fantasy.football.service

import com.fantasy.football.config.Config
import com.fantasy.football.models.Message
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.logging.LogLevel
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import service.YahooClient

class BotService {

    companion object {
        val config = ConfigLoaderBuilder
            .default()
            .addResourceSource("/application.toml").build().loadConfigOrThrow<Config>()
    }

    private lateinit var bot: Bot

    private val yahoo = YahooApiService(YahooClient())
    private val telegramService by lazy {
        TelegramService(bot)
    }

    private suspend fun setupBot() {
        bot = bot {
            token = config.telegram.token
            logLevel = LogLevel.Network.Body
            dispatch {
                commands().forEach {
                    command(it.commandName) { it.command.invoke(update.message!!.chat.id.toString()).value }
                }
                command("search") {
                    val playerName = args.joinToString().replace(",", "")
                    GlobalScope.launch(Dispatchers.IO) {
                        telegramService.sendMessage(yahoo.searchPlayer(playerName), update.message!!.chat.id.toString())
                    }
                }
            }
        }
    }

    suspend fun startBot() {
        setupBot()
        bot.startPolling()
        scheduleMessages()
        scheduleBotUpdates()
    }

    private fun scheduleMessages() {
        commands().filter { it.scheduled }.forEach { message ->
            EventNotificationScheduler(config.telegram.timezone)
                .scheduleNextExecution(message.command, message.schedules, config.telegram.announcementChatId)
        }
    }

    private fun scheduleBotUpdates() {
        EventNotificationScheduler(config.telegram.timezone)
            .scheduleNextExecution(
            { yahoo.updateCurrentWeek() },
            Pair("every tue 06:00", "Update Current Week"),
        )
    }

    private fun commands(): List<Message> = listOf(
        Message(
            "commands",
            { chatId: String -> lazy { telegramService.sendMessage(TelegramService.COMMANDS, chatId) } }
        ),
        Message(
            "schedule",
            { chatId: String -> lazy { telegramService.sendMessage(TelegramService.SCHEDULE, chatId) } }
        ),
        Message(
            "scores",
            { chatId: String -> lazy { telegramService.sendMessage(yahoo.getScoreBoard(), chatId) } },
            true,
            listOf(
                Pair("every sun 15:00", "Score Update PM"),
                Pair("every sun 19:00", "Score Update PM Sunday"),
                Pair("every fri,mon 07:30", "Score Update AM")

            )
        ),
        Message(
            "proj",
            { chatId: String -> lazy { telegramService.sendMessage(yahoo.getScoreBoard(projections = true), chatId) } },
            true,
            listOf(Pair("every wed 07:30", "Score Projections"))
        ),

        Message(
            "final",
            { chatId: String -> lazy { telegramService.sendMessage(yahoo.getScoreBoard(final = true), chatId) } },
            true,
            listOf(Pair("every tue 07:30", "Final Score"))
        ),
        Message(
            "matchups",
            { chatId: String -> lazy { telegramService.sendMessage(yahoo.getMatchups(), chatId) } },
            true,
            listOf(Pair("every thu 18:30", "Matchups"))
        ),
        Message(
            "standings",
            { chatId: String -> lazy { telegramService.sendMessage(yahoo.getStandings(), chatId) } },
            true,
            listOf(Pair("every wed 07:30", "Current Standings"))
        ),
        Message(
            "waiver",
            { chatId: String -> lazy { telegramService.sendMessage(yahoo.getTransactions(), chatId) } },
            true,
            listOf(Pair("every wed 07:30", "Waiver Report"))
        ),
        Message(
            "monitor",
            { chatId: String -> lazy { telegramService.sendMessage(yahoo.getMonitor(true), chatId) } },
            true,
            listOf(
                Pair("every thu 18:30", "Player Monitor"),
                Pair("every mon 18:30", "Player Monitor"),
                Pair("every sunday 07:30", "Player Monitor")
            )
        ),
        Message(
            "closeScore",
            { chatId: String -> lazy { telegramService.sendMessage(yahoo.getCloseScores(), chatId) } },
            true,
            listOf(Pair("every mon 18:30", "Close Scores"))
        )
    )
}
