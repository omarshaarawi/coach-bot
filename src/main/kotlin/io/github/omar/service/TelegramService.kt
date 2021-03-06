package io.github.omar.service

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.network.fold
import io.github.omar.model.TelegramConfig
import mu.KotlinLogging

class TelegramService(private val bot: Bot, private val telegramConfig: TelegramConfig) {

    fun sendMessage(string: String) {
        val chatId = ChatId.fromId(telegramConfig.chatID)
        val newString = escapeInvalidChars(string)

        val messageResponse =
            bot.sendMessage(chatId, parseMode = ParseMode.MARKDOWN_V2, text = newString)

        messageResponse.fold({
            LOGGER.info { "Message Sent Successfully" }
        }, {
            LOGGER.error { "Code: ${messageResponse.first?.code()} Body: ${it.errorBody?.string()}" }
            sendMessage("Message Failed To Send")
        })
    }

    private fun escapeInvalidChars(string: String): String {
        return string
            .replace(".", "\\.")
            .replace("!", "\\!")
            .replace("-", "\\-")
            .replace("(", "\\(")
            .replace(")", "\\)")
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }

}
