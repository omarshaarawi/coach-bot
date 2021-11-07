package io.github.omar.model

data class TelegramConfig(
    val token: String,
    val chatID: Long,
    val timezone: String,
)
