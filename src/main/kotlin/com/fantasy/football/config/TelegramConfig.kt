package com.fantasy.football.config

import com.sksamuel.hoplite.ConfigAlias

data class TelegramConfig(
    val token: String,
    @ConfigAlias("chat_id")
    val chatId: String,
    val timezone: String
)
