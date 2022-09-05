package com.fantasy.football.service

import FantasyContentResource
import models.TeamsResource
import service.YahooClient

class YahooApiService(private val yahooClient: YahooClient) {

    private var numberOfTeams: Int?
    private var teamNameKeyMap = mutableMapOf<Int, String>()

    init {
        val league = yahooClient.getLeagueTeams()
        numberOfTeams = league?.numTeams
        league?.teams?.forEach {
            teamNameKeyMap[it.teamId] = it.name
        }
    }

    private fun getLeague(): FantasyContentResource {
        return yahooClient.getLeague()
    }

    fun getScoreBoard(final: Boolean = false, projections: Boolean = false): String {
        val scores = mutableListOf<String>()

        yahooClient.getScoreboard()?.matchups?.forEach {
            val team1 = it.teams[0]
            val team2 = it.teams[1]
            if (projections) {
                scores.add(
                    "${team1.name} ${team1.teamProjectedPoints?.total}" +
                        " - ${team2.name} ${team2.teamProjectedPoints?.total}"
                )
            } else {
                scores.add(
                    "${team1.name} ${team1.teamPoints?.total}" +
                        " - ${team2.name} ${team2.teamPoints?.total}"
                )
            }
        }
        return scores.joinToString(
            prefix = scoreboardPrefix(final, projections),
            separator = "\n\n"
        )
    }

    private fun scoreboardPrefix(final: Boolean, projections: Boolean): String {
        return if (final) {
            "*Final Scores*\n\n"
        } else if (projections) {
            "*Projected Scores*\n\n"
        } else "*Current Scores*\n\n"
    }

    fun getStandings(): String {
        val standings = mutableListOf<String>()
        yahooClient.getLeague().league?.standings?.teams?.forEach {
            standings.add("${it.teamStandings?.rank}. ${it.name}")
        }
        return standings.joinToString(prefix = "*Standings*\n\n", separator = "\n\n")
    }

    fun getMatchups(): String {
        val matchups = mutableListOf<String>()
        val league = yahooClient.getAllLeagueResources().league
        val standings = league?.standings?.teams
        val recordsMap = getTeamRecordsById(standings)

        yahooClient.getAllLeagueResources().league?.scoreboard?.matchups?.forEach {
            val team1 = it.teams[0]
            val team2 = it.teams[1]

            matchups.add(
                "${team1.name} ${recordsMap[team1.teamId]} " +
                    "vs ${team2.name} ${recordsMap[team2.teamId]}"
            )
        }
        return matchups.joinToString(prefix = "*This Week's Matchups*\n\n", separator = "\n\n")
    }

    private fun getTeamRecordsById(standings: List<TeamsResource>?): Map<Int, String> {
        return standings!!.associate {
            it.teamId to
                "(${it.teamStandings?.outcomeTotals?.wins}" +
                "-${it.teamStandings?.outcomeTotals?.ties}" +
                "-${it.teamStandings?.outcomeTotals?.losses})"
        }
    }
}
