package com.fantasy.football.models

import java.time.LocalDate

data class NFLGame(
    val teams: List<String>,
    val hasPlayed: Boolean,
    val date: LocalDate,
)
