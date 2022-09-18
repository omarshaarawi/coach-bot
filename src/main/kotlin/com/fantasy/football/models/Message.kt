package com.fantasy.football.models

class Message(
    val commandName: String,
    val command: (String) -> Lazy<Unit>,
    val scheduled: Boolean = false,
    val schedules: List<Pair<String, String>>? = null

)
