package com.fantasy.football.config

import config.YahooConfig

data class Config(
    val telegram: TelegramConfig,
    val yahoo: YahooConfig,
    val db: DatabaseConfig,
    val ktor: KtorConfig
)
