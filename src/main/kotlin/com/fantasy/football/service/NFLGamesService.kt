package com.fantasy.football.service

import com.fantasy.football.models.NFLGames
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class NFLGamesService {
    private val client = HttpClient {
        expectSuccess = true
        install(Logging)
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                }
            )
        }
    }

    suspend fun getGames(): Map<String, Boolean> {
        val response: HttpResponse =
            client.request("https://site.api.espn.com/apis/site/v2/sports/football/nfl/scoreboard") {
                method = HttpMethod.Get
            }

        val games: Map<String, Boolean> = response.body<NFLGames>().events.flatMap { competitions ->
            competitions.competitions.flatMap { competitors ->
                competitors.competitors.map { it.team.displayName to competitions.status.type.completed }
            }
        }.toMap()

        return games
    }
}
