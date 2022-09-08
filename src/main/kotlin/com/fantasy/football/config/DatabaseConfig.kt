package com.fantasy.football.config

import com.sksamuel.hoplite.Masked

data class DatabaseConfig(val user: String, val password: Masked, val url: String)
