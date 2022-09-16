package com.fantasy.football.models

data class TeamRosters(
    val teamName: String,
    val roster: List<Player>
) {
    data class Player(
        val name: String,
        val teamName: String,
        val position: String,
        val status: String?,
        val selectedPosition: String?,
        val team: String,
        val hasPlayed: Boolean
    )
}
