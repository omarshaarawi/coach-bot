package com.fantasy.football.models

import kotlinx.serialization.Serializable

@Serializable
data class NFLGames(val events: List<Event>) {
    @Serializable
    data class Event(val competitions: List<Competitions>, val status: Status)

    @Serializable
    data class Status(val type: Type)

    @Serializable
    data class Type(val completed: Boolean)

    @Serializable
    data class Competitions(val date: String, val competitors: List<Competitors>)

    @Serializable
    data class Competitors(val team: Team)

    @Serializable
    data class Team(val displayName: String)
}
