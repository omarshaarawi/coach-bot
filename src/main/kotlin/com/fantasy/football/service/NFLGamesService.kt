package com.fantasy.football.service

import com.fantasy.football.models.NFLGame
import com.fantasy.football.models.NFLGamesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime
import java.time.ZoneId

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

    suspend fun getGames(): List<NFLGame> {
        val response: HttpResponse =
            client.request("https://site.api.espn.com/apis/site/v2/sports/football/nfl/scoreboard") {
                method = HttpMethod.Get
            }

        val nflGamesResponse: NFLGamesResponse = response.body()
        val timeZone = ZoneId.of("America/Chicago")

        val games = nflGamesResponse.events.map { event ->
            val teams = listOf(
                event.competitions[0].competitors[0].team.displayName,
                event.competitions[0].competitors[1].team.displayName
            )
            val status = event.status.type.completed
            val date = OffsetDateTime.parse(event.competitions[0].date).atZoneSameInstant(timeZone).toLocalDate()
            NFLGame(teams, status, date)
        }

        return games
    }
}
