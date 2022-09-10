package com.fantasy.football.service

import FantasyContentResource // ktlint-disable import-ordering
import com.fantasy.football.models.TeamRosters
import com.fantasy.football.models.TeamRosters.Player
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import models.TeamsResource
import service.YahooClient

class YahooApiService(private val yahooClient: YahooClient) {

    private var numberOfTeams: Int?
    private var teamNameKeyMap = mutableMapOf<Int, String>()
    private val nflGamesService = NFLGamesService()

    companion object {
        var LOW_SCORE = 9999.0
        var HIGH_SCORE = -1.0
        var CLOSEST_SCORE = 9999.0
        var BIGGEST_BLOWOUT = -1.0
        const val DATE_FACTOR = 1000
        const val MIN_CLOSE_SCORE_DIFF = -16
        const val MAX_CLOSE_SCORE_DIFF = 16
        const val MIN_CLOSE_SCORE = 0
    }

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

    private fun getTrophies(): String {
        var lowScore = LOW_SCORE
        var lowTeamName = ""
        var highScore = HIGH_SCORE
        var highTeamName = ""
        var closestScore = CLOSEST_SCORE
        var closeWinner = ""
        var closeLoser = ""
        var biggestBlowout = BIGGEST_BLOWOUT
        var blownOutTeamName = ""
        var ownererTeamName = ""
        yahooClient.getScoreboard()?.matchups?.forEach { matchup ->
            val homeTeam = matchup.teams[0]
            val awayTeam = matchup.teams[1]

            val homeTeamScore = homeTeam.teamPoints!!.total
            val awayTeamScore = awayTeam.teamPoints!!.total

            if (homeTeamScore > highScore) {
                highScore = homeTeamScore
                highTeamName = teamNameKeyMap[homeTeam.teamId]!!
            }
            if (homeTeamScore < lowScore) {
                lowScore = homeTeamScore
                lowTeamName = teamNameKeyMap[homeTeam.teamId]!!
            }
            if (awayTeamScore > highScore) {
                highScore = awayTeamScore
                highTeamName = teamNameKeyMap[awayTeam.teamId]!!
            }
            if (awayTeamScore < lowScore) {
                lowScore = awayTeamScore
                lowTeamName = teamNameKeyMap[awayTeam.teamId]!!
            }
            if (awayTeamScore - homeTeamScore != 0.0 && (abs(awayTeamScore - homeTeamScore) < closestScore)) {
                closestScore = abs(awayTeamScore - homeTeamScore)
                if (awayTeamScore - homeTeamScore < 0) {
                    closeWinner = teamNameKeyMap[homeTeam.teamId]!!
                    closeLoser = teamNameKeyMap[awayTeam.teamId]!!
                } else {
                    closeWinner = teamNameKeyMap[awayTeam.teamId]!!
                    closeLoser = teamNameKeyMap[homeTeam.teamId]!!
                }
            }
            if (abs(awayTeamScore - homeTeamScore) > biggestBlowout) {
                biggestBlowout = abs(awayTeamScore - homeTeamScore)
                if (awayTeamScore - homeTeamScore < 0) {
                    ownererTeamName = teamNameKeyMap[homeTeam.teamId]!!
                    blownOutTeamName = teamNameKeyMap[awayTeam.teamId]!!
                } else {
                    ownererTeamName = teamNameKeyMap[awayTeam.teamId]!!
                    blownOutTeamName = teamNameKeyMap[homeTeam.teamId]!!
                }
            }
        }

        val lowScoreString = "Low score: $lowTeamName with $lowScore points"
        val highScoreString = "High score: $highTeamName with $highScore points"
        val closeScoreString = "$closeWinner barely beat $closeLoser by a margin of $closestScore"
        val blowoutString = "$blownOutTeamName blown out by $ownererTeamName by a margin of $biggestBlowout"
        return """
                *Trophies of the week:*
                $lowScoreString
                $highScoreString
                $closeScoreString
                $blowoutString
            """.trimIndent()
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
        if (final) {
            scores.add(getTrophies())
        }
        return scores.joinToString(prefix = scoreboardPrefix(final, projections), separator = "\n\n")
    }

    fun getStandings(): String {
        val standings = mutableListOf<String>()
        yahooClient.getStandings()?.teams?.forEach {
            standings.add("${it.teamStandings?.rank}: ${it.name} ${getTeamRecord(it)}")
        }

        return standings.sorted().joinToString(prefix = "*Current Standings*\n\n", separator = "\n")
    }

    fun getMatchups(): String {
        val matchups = mutableListOf<String>()
        val league = yahooClient.getAllLeagueResources().league
        val standings = league?.standings?.teams
        val recordsMap = getTeamRecordsById(standings)

        yahooClient.getAllLeagueResources().league?.scoreboard?.matchups?.forEach {
            val team1 = it.teams[0]
            val team2 = it.teams[1]

            matchups.add("${team1.name} ${recordsMap[team1.teamId]} " + "vs ${team2.name} ${recordsMap[team2.teamId]}")
        }
        return matchups.joinToString(prefix = "*This Week's Matchups*\n\n", separator = "\n\n")
    }

    fun getTransactions(): String {
        val transactions = mutableListOf<String>()
        val today = LocalDate.now().toString()
        yahooClient.getTransactions()!!.filter { transaction
            ->
            getDateTime(transaction.timestamp.toString()) == today
        }.forEach {
            it.players?.forEach { player ->
                when (player.transactionData?.type) {
                    "add" -> {
                        transactions.add(
                            "${player.transactionData!!.destinationTeamName} \nADDED " +
                                "${player.displayPosition} ${player.name.full}\n".trim()
                        )
                    }

                    "drop" -> {
                        transactions.add(
                            "${player.transactionData!!.sourceTeamName} \nDROPPED " +
                                "${player.displayPosition} ${player.name.full}\n".trim()
                        )
                    }
                }
            }
        }

        if (transactions.isEmpty()) {
            return "No waiver transactions today"
        }
        return transactions.joinToString(prefix = "*Waiver Report: $today*\n\n", separator = "\n\n")
    }

    fun getMonitor(): String {
        val rosters = runBlocking { getTeamRosters() }
        val monitor = mutableListOf<String>()

        rosters.forEach { team ->
            monitor += scanRoster(team.roster)
        }

        val text: String = if (monitor.isNotEmpty()) {
            "*Starting Players to Monitor:*\n\n"
        } else {
            "*No Players to Monitor this week. Good Luck!*"
        }
        return monitor.filter { it.isNotBlank() }.joinToString(prefix = text, separator = "\n")
    }

    fun getCloseScores(): String {
        val matchups = yahooClient.getScoreboard()!!.matchups
        val rosters = runBlocking { getTeamRosters() }
        var score = mutableListOf<String>()

        matchups.forEach { matchup ->
            val team1 = matchup.teams[0]
            val team2 = matchup.teams[1]
            val team1Roster = rosters.find { it.teamName == team1.name }!!.roster
            val team2Roster = rosters.find { it.teamName == team2.name }!!.roster
            val diffScore = team1.teamPoints!!.total - team2.teamPoints!!.total

            if ((MIN_CLOSE_SCORE_DIFF < diffScore && !allPlayed(team2Roster)) ||
                MIN_CLOSE_SCORE <= diffScore && diffScore < MAX_CLOSE_SCORE_DIFF && !allPlayed(team1Roster)
            ) {
                score.add(
                    "%s %.2f - %.2f %s".format(
                        team1.name,
                        team1.teamPoints!!.total,
                        team2.teamPoints!!.total,
                        team2.name
                    )
                )
            }
        }
        if (score.isEmpty()) {
            score = mutableListOf("None")
        }
        return score.joinToString(prefix = "*Close Scores*\n\n", separator = "\n\n")
    }

    private fun scanRoster(roster: List<Player>): String {
        val players = mutableListOf<String>()
        roster.forEach { player ->
            if (player.status == "O" || player.status == "D" || player.status == "Q") {
                players.add("${player.position} ${player.name} - ${player.status}")
            }
        }
        return players.joinToString(separator = "\n")
    }

    private fun allPlayed(roster: List<Player>): Boolean {
        roster.forEach { player ->
            if (player.selectedPosition == "BN" || player.status == "IR") {
                return false
            }
        }
        return true
    }

    private suspend fun getTeamRosters(): List<TeamRosters> {
        val nflGames = nflGamesService.getGames()
        return yahooClient.getTeams()!!.map { team ->
            val listOfPlayers = mutableListOf<Player>()
            team.roster!!.players.forEach { player ->
                val hasPlayed = nflGames.getValue(player.editorialTeamFullName!!)
                listOfPlayers.add(
                    Player(
                        player.name.full,
                        player.displayPosition!!,
                        player.status,
                        player.selectedPosition!!.position,
                        player.editorialTeamFullName!!,
                        hasPlayed
                    )
                )
            }
            TeamRosters(team.name, listOfPlayers)
        }
    }

    private fun getDateTime(s: String): String? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd")
            val netDate = Date(s.toLong() * DATE_FACTOR)
            sdf.format(netDate)
        } catch (e: Exception) {
            e.toString()
        }
    }

    private fun getTeamRecord(standings: TeamsResource): String {
        return "(${standings.teamStandings?.outcomeTotals?.wins}" +
            "-${standings.teamStandings?.outcomeTotals?.ties}" + "-${standings.teamStandings?.outcomeTotals?.losses})"
    }

    private fun getTeamRecordsById(standings: List<TeamsResource>?): Map<Int, String> {
        return standings!!.associate {
            it.teamId to "(${it.teamStandings?.outcomeTotals?.wins}" +
                "-${it.teamStandings?.outcomeTotals?.ties}" + "-${it.teamStandings?.outcomeTotals?.losses})"
        }
    }

    private fun scoreboardPrefix(final: Boolean, projections: Boolean): String {
        return if (final) {
            "*Final Scores*\n\n"
        } else if (projections) {
            "*Projected Scores*\n\n"
        } else "*Current Scores*\n\n"
    }
}
