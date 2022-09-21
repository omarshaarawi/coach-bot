package com.fantasy.football.service

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.network.fold
import mu.KotlinLogging

class TelegramService(private val bot: Bot) {
    fun sendMessage(string: String, chatId: String) {
        if (string == "N/A") {
            return
        }
        val newString = escapeInvalidChars(string)
        val id = ChatId.fromId(chatId.toLong())
        val messageResponse =
            bot.sendMessage(id, parseMode = ParseMode.MARKDOWN_V2, text = newString)

        messageResponse.fold({
            LOGGER.info { "Message Sent Successfully" }
        }, {
            LOGGER.error { "Code: ${messageResponse.first?.code()} Body: ${it.errorBody?.string()}" }
        })
    }

    private fun escapeInvalidChars(string: String): String {
        return string
            .replace(".", "\\.")
            .replace("!", "\\!")
            .replace("-", "\\-")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("=", "\\=")
    }

    companion object {
        val COMMANDS = """
            /scores - Get the current scores
            /final - Get the final scores
            /proj - Get the projected scores
            /standings - Get the current standings
            /matchups - Get the current matchups
            /waivers - Get today's waivers
            /monitor - Players in starting lineup that are Questionable, Doubtful, or Out
            /search - Search for who has a player
        """.trimIndent()

        val SCHEDULE = """
            Player Monitor - every sunday 7:30am 
            Player Monitor - every monday & thursday 6:30pm
            Waivers - every wednesday 7:30am
            Standings - every wednesday 7:30am
            Matchups - every thursday 6:30pm
            Scores - every friday 7:30am
            Scores - every sunday 3:00pm & 7:00pm
            Final Scores - every tuesday 7:30am
            Projected Scores - every wednesday 7:30am
        """.trimIndent()

        private val LOGGER = KotlinLogging.logger { }
    }
}
