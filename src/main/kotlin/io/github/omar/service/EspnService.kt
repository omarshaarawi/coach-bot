package io.github.omar.service

import com.espn.ff.model.Activity
import com.espn.ff.model.BoxPlayer
import com.espn.ff.model.BoxScore
import com.espn.ff.model.League
import com.espn.ff.model.Player
import com.espn.ff.model.Team
import io.github.omar.model.ESPNConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.abs
import me.xdrop.fuzzywuzzy.FuzzySearch

class EspnService(espnConfig: ESPNConfig) {

    private val league =
        League(
            espnConfig.leagueId,
            Calendar.getInstance().get(Calendar.YEAR),
            espnConfig.swid,
            espnConfig.espnS2
        )

    fun getCurrentWeek(): Int {
        return league.currentWeek
    }

    fun getTrophies(week: Int): String {
        val matchups = league.boxScore(week = week)
        var lowScore = 9999.0
        var lowTeamName = ""
        var highScore = -1.0
        var highTeamName = ""
        var closestScore = 9999.0
        var closeWinner = ""
        var closeLoser = ""
        var biggestBlowout = -1.0
        var blownOutTeam = ""
        var ownererTeamName = ""


        for (i in matchups) {
            if (i.homeScore > highScore) {
                highScore = i.homeScore
                highTeamName = (i.homeTeam as Team).teamName
            }
            if (i.homeScore < lowScore) {
                lowScore = i.homeScore
                lowTeamName = (i.homeTeam as Team).teamName
            }
            if (i.awayScore > highScore) {
                highScore = i.awayScore
                highTeamName = (i.awayTeam as Team).teamName
            }
            if (i.awayScore < lowScore) {
                lowScore = i.awayScore
                lowTeamName = (i.awayTeam as Team).teamName
            }
            if (i.awayScore - i.homeScore != 0.0 && abs(i.awayScore - i.homeScore) < closestScore) {
                closestScore = abs(i.awayScore - i.homeScore)
                if (i.awayScore - i.homeScore < 0) {
                    closeWinner = (i.homeTeam as Team).teamName
                    closeLoser = (i.awayTeam as Team).teamName
                } else {
                    closeWinner = (i.awayTeam as Team).teamName
                    closeLoser = (i.homeTeam as Team).teamName
                }
            }
            if (abs(i.awayScore - i.homeScore) > biggestBlowout) {
                biggestBlowout = abs(i.awayScore - i.homeScore)
                if (i.awayScore - i.homeScore < 0.0) {
                    ownererTeamName = (i.homeTeam as Team).teamName
                    blownOutTeam = (i.awayTeam as Team).teamName
                } else {
                    ownererTeamName = (i.awayTeam as Team).teamName
                    blownOutTeam = (i.homeTeam as Team).teamName
                }
            }
        }

        val lowestScoreStr = "Lowest score: %s with %.2f points".format(lowTeamName, lowScore)
        val highestScoreStr = "Highest score: %s with %.2f points".format(highTeamName, highScore)
        val closeScoreStr = "%s barely beat %s by a margin of %.2f points".format(closeWinner,
            closeLoser,
            closestScore)

        val blowoutStr =
            "%s blown out by %s by a margin of %.2f points".format(blownOutTeam, ownererTeamName, biggestBlowout)

        return """

            *Trophies:*
            
            $lowestScoreStr

            $highestScoreStr

            $closeScoreStr

            $blowoutStr
        """.trimIndent().replace("\"", "")
    }

    fun scoreboard(final: Boolean = false): String {
        return getScoreBoard(final = final)
    }

    fun getPowerRankings(): String {
        val powerRankings = league.powerRankings()
        val score: MutableList<String> = mutableListOf()
        for (i in powerRankings) {
            score.add("""
                
               ${i.first} - ${i.second.teamName.replace("\"", "")}
            """.trimIndent())
        }

        return score.joinToString(prefix = "\n*Power Rankings:*\n").replace(",", "")
    }

    private fun getScoreBoard(final: Boolean): String {
        val matchups: MutableList<BoxScore>
        val textHeader: String
        val message: String

        if (!final) {
            matchups = league.boxScore()
            textHeader = "*Score Update*\n\n"
        } else {
            matchups = league.boxScore(getCurrentWeek())
            textHeader = "*Final Score*\n\n"
        }


        val score = mutableListOf<String>()

        for (i in matchups) {
            score.add("%s %.2f - %.2f %s\n\n".format(
                (i.homeTeam as Team).teamAbbrev.replace("\"", ""),
                i.homeScore,
                i.awayScore,
                (i.awayTeam as Team).teamAbbrev.replace("\"", "")).trimIndent())
        }


        message = if (!final) {
            score.joinToString(prefix = textHeader, separator = "\n").replace(",", "")
        } else {
            val trophies = getTrophies(getCurrentWeek())
            score.joinToString(prefix = textHeader, postfix = trophies, separator = "\n").replace(",", "")
        }

        return message
    }

    fun getProjectedScoreboard(): String {
        val score = mutableListOf<String>()

        val boxScore = league.boxScore()

        for (i in boxScore) {
            score.add("%s %.2f - %.2f %s".format((i.homeTeam as Team).teamAbbrev.replace("\"", ""),
                getProjectedTotal(i.homeLinup),
                getProjectedTotal(i.awayLineup),
                (i.awayTeam as Team).teamAbbrev.replace("\"", "")))
        }

        return score.joinToString(prefix = "*Projected Scores*\n\n", separator = "\n\n")
    }


    fun refresh() {
        league.refresh()
    }

    private fun getProjectedTotal(lineup: MutableList<BoxPlayer>): Double {
        var totalProjected = 0.0

        for (i in lineup) {
            if (i.slotPosition != "BE" && i.slotPosition != "IR") {
                totalProjected = if (i.points != 0.0 || i.gamePlayed > 0) {
                    totalProjected + i.points
                } else {
                    totalProjected + i.projectedPoints
                }
            }
        }
        return totalProjected
    }

    private fun allPlayed(lineup: MutableList<BoxPlayer>): Boolean {
        for (i in lineup) {
            if (i.slotPosition != "BE" && i.slotPosition != "IR" && i.gamePlayed < 100) {
                return false
            }
        }
        return true
    }

    fun getMatchups(): String {
        val matchups = league.boxScore()

        val score = mutableListOf<String>()

        for (i in matchups) {
            val homeTeam = i.homeTeam as Team
            val awayTeam = i.awayTeam as Team
            score.add("%s(%s-%s) vs %s(%s-%s)".format(
                homeTeam.teamName.replace("\"", ""),
                homeTeam.wins,
                homeTeam.losses,
                awayTeam.teamName.replace("\"", ""),
                awayTeam.wins,
                awayTeam.losses
            ))
        }

        return score.joinToString(prefix = "*This Week's Matchups*\n\n", separator = "\n\n")
    }

    fun getCloseScores(): String {
        val matchups = league.boxScore()
        var score = mutableListOf<String>()

        for (i in matchups) {
            val homeTeam = i.homeTeam as Team
            val awayTeam = i.awayTeam as Team
            val diffScore = i.awayScore - i.homeScore
            if ((-16 < diffScore && !allPlayed(i.awayLineup)) || (0 <= diffScore && diffScore < 16 && !allPlayed(i.homeLinup))) {
                score.add("%s %.2f - %.2f %s".format(
                    homeTeam.teamAbbrev.replace("\"", ""),
                    i.homeScore,
                    i.awayScore,
                    awayTeam.teamAbbrev.replace("\"", "")
                ))
            }
        }

        if (score.isEmpty()) {
            score = mutableListOf("None")
        }
        return score.joinToString(prefix = "*Close Scores*\n\n", separator = "\n\n")
    }

    private fun getTeamRoster(): MutableMap<String, String> {
        val takenPlayers = mutableMapOf<String, String>()
        val magdy = league.teams[0]
        val omar = league.teams[1]
        val ashok = league.teams[2]
        val nolan = league.teams[3]
        val michael = league.teams[4]
        val vince = league.teams[5]

        for (team in league.teams) {
            for (player in team.roster) {
                when (team) {
                    magdy -> takenPlayers[player.name] = "Magdy"
                    omar -> takenPlayers[player.name] = "Omar"
                    ashok -> takenPlayers[player.name] = "Ashok"
                    nolan -> takenPlayers[player.name] = "Nolan"
                    michael -> takenPlayers[player.name] = "Michael"
                    vince -> takenPlayers[player.name] = "Vince¬"
                }
            }
        }

        return takenPlayers

    }

    private fun freeAgents(): MutableList<String> {
        val listOfFreeAgents = mutableListOf<String>()
        val freeAgents = league.freeAgents(size = 300)
        for (player in freeAgents) {
            listOfFreeAgents.add(player.name)
        }

        return listOfFreeAgents
    }

    private fun getPointsForPlayer(playerName: String, isFreeAgent: Boolean): String {
        val boxScores = league.boxScore()
        val freeAgents = league.freeAgents(size = 300)
        var k = 0
        var message = ""

        if (isFreeAgent) {
            while (k < freeAgents.size) {
                if (playerName == freeAgents[k].name) {
                    message = """
                        *Actual:* ${freeAgents[k].points}
                        *Projected:* ${freeAgents[k].projectedPoints}
                    """.trimIndent()
                }
                k += 1
            }
            return message
        } else {
            var i = 0
            while (i < boxScores.size) {
                val homeLineup = checkHomeLineupForPlayer(playerName, boxScores[i])
                val awayLineup = checkAwayLineupForPlayer(playerName, boxScores[i])
                if (homeLineup != null) {
                    message = """
                        *Actual:* ${homeLineup.first}
                        *Projected:* ${homeLineup.second}
                        """.trimIndent()
                } else if (awayLineup != null) {
                    message = """
                        *Actual:* ${awayLineup.first}
                        *Projected:* ${awayLineup.second}
                        """.trimIndent()
                }
                i += 1
            }
        }
        return message
    }

    fun getStandings(): String {
        val standings = league.standings()
        val standingsStringList = mutableListOf<String>()
        standings.forEachIndexed { pos, team ->
            val message = "%s: %s (%s)-(%s)\n".format(pos, team.teamName, team.wins, team.losses)
            standingsStringList.add(message)
        }
        return standingsStringList.joinToString(prefix = "**Current Standings:**\n", separator = "\n")
    }

    fun getWaiverReport(): String {
        val activities: MutableList<Activity> = league.recentActivity(size = 50) as MutableList<Activity>
        val report: MutableList<String> = mutableListOf()
        val s = SimpleDateFormat("YYYY-M-dd")
        val text: List<String>
        val today: String = s.format(Date())
        var a: String

        for (activity in activities) {
            val actions = activity.actions
            val d2 = s.format(
                Date.from(activity.date))

            if (d2 == today) {
                if (actions.size == 1 && actions[0][1] == "WAIVER ADDED") {
                    a = """
                        __${(actions[0][0] as Team).teamName}__
                        ADDED ${(actions[0][2] as Player).position} ${(actions[0][2] as Player).name}
                       
                    """.trimIndent().replace("\"", "")
                    report += listOf(a)
                } else if (actions.size > 1) {
                    if (actions[0][1] == "WAIVER ADDED" || actions[1][1] == "WAIVER ADDED") {
                        if (actions[0][1] == "WAIVER ADDED") {
                            a = """
                                __${(actions[0][0] as Team).teamName}__
                                ADDED ${(actions[0][2] as Player).position} ${(actions[0][2] as Player).name}
                                DROPPED ${(actions[1][2] as Player).position} ${(actions[1][2] as Player).name}
                                
                            """.trimIndent().replace("\"", "")
                        } else {

                            a = """
                                __${(actions[0][0] as Team).teamName}__
                                ADDED ${(actions[1][2] as Player).position} ${(actions[1][2] as Player).name}
                                DROPPED ${(actions[0][2] as Player).position} ${(actions[0][2] as Player).name}
                                
                            """.trimIndent().replace("\"", "")
                        }
                        report += listOf(a)
                    }
                }

            }
        }
        report.reverse()

        text = if (report.isEmpty()) {
            listOf("*No waiver transactions*")
        } else {
            listOf("*Waiver Report %s:* \n".format(today)) + report
        }

        return text.joinToString(separator = "\n")
    }


    fun getMonitor(): String {
        val boxScores = league.boxScore()
        val monitor = mutableListOf<String>()

        for (i in boxScores) {
            monitor += scanRoster(i.homeLinup, (i.homeTeam as Team))
            monitor += scanRoster(i.awayLineup, (i.awayTeam as Team))
        }
        val text: String = if (monitor.isNotEmpty()) {
            "*Starting Players to Monitor:*"
        } else {
            "*No Players to Monitor this week. Good Luck!*"
        }

        return monitor.joinToString(prefix = text).replace(",", "")
    }

    private fun scanRoster(lineup: MutableList<BoxPlayer>, team: Team): List<String> {
        var count = 0
        val players = mutableListOf<String>()

        for (i in lineup) {
            if (i.slotPosition.replace("\"", "") != "BE" && i.slotPosition.replace("\"", "") != "IR"
                && i.injuryStatus.replace("\"", "") != "ACTIVE" && i.injuryStatus.replace("\"", "") != "NORMAL"
                && i.gamePlayed == 0
            ) {
                count += 1
                val player = "${i.position} ${i.name} - ${i.injuryStatus.replace("_", " ").replace("\"", "")}"
                players += listOf(player)
            }
        }
        var list = ""
        var report = emptyList<String>()

        for (p in players) {
            list += p + "\n"
        }
        if (count > 0) {
            val s = """
 
 
__${team.teamName.replace("\"", "")}:__
${list.slice(list.indices)}
""".trimIndent()
            report = listOf(s)
        }

        return report
    }


    private fun getSimplePoints(playerName: String): Double {
        val boxScore = league.boxScore(week = 4)
        var message = 0.0
        var i = 0
        while (i < boxScore.size) {

            val homeLineup = checkHomeLineupForPlayer(playerName, boxScore[i])


            if (homeLineup != null) {

                message = homeLineup.first

            } else checkAwayLineupForPlayer(playerName, boxScore[i])?.first

            i += 1
        }
        return message
    }

    fun whoHas(playerName: String): String {
        val teamRoster = getTeamRoster()
        val message: String
        val freeAgent = freeAgents()
        val newPlayerName: String
        val points: String

        val y = FuzzySearch.extractOne(playerName, teamRoster.keys)
        val x = FuzzySearch.extractOne(playerName, freeAgent)
        if (y.score > 75 && y.string.startsWith(playerName.take(1), ignoreCase = true)) {
            newPlayerName = y.string
            points = getPointsForPlayer(newPlayerName, false)
            message =
                """
${teamRoster[newPlayerName]} has $newPlayerName

$points
""".trimIndent()
        } else if (x.score > 75 && x.string.startsWith(playerName.take(1))) {
            newPlayerName = x.string
            points = getPointsForPlayer(newPlayerName, true)
            message = "%s is a free agent\n %s".format(newPlayerName, points)
        } else {
            message = "Who?"
        }

        return message
    }

    fun breakoutAlert(): String {
        val breakoutList = mutableListOf<String>()
        val breakoutInnerList = mutableListOf<String>()
        val freeAgents = league.freeAgents(size = 100)
        val teamRoster = getTeamRoster()
        var message = ""

        for (player in teamRoster) {
            val main = breakoutList.minus(teamRoster)
            if (player.key !in main && getSimplePoints(player.key) >= 20) {
                message = """
                    **Breakout Alert**
                    %s (%s)
                    
                    %s Points
                """.trimIndent().format(player.key, teamRoster[player.key], getSimplePoints(player.key))

                breakoutList.add(player.key)
                breakoutInnerList.add(player.key)

            }
        }

        for (player in freeAgents) {
            val main = teamRoster.minus(breakoutList.toSet())
            if (player.name !in main && getSimplePoints(player.name) >= 20) {
                message = """
                    **Breakout Alert**
                    %s
                    (FA)

                    %s Points
                """.trimIndent().format(player, teamRoster[player.name], getSimplePoints(player.name))
            }
        }
        return message
    }

    private fun checkHomeLineupForPlayer(playerName: String, boxScore: BoxScore): Pair<Double, Double>? {
        boxScore.homeLinup.forEachIndexed { index, _ ->
            if (boxScore.homeLinup[index].name == playerName) {
                return Pair(boxScore.homeLinup[index].points, boxScore.homeLinup[index].projectedPoints)
            }
        }

        return null

    }

    private fun checkAwayLineupForPlayer(playerName: String, boxScore: BoxScore): Pair<Double, Double>? {
        boxScore.awayLineup.forEachIndexed { index, _ ->
            if (boxScore.awayLineup[index].name == playerName) {
                return Pair(boxScore.awayLineup[index].points, boxScore.awayLineup[index].projectedPoints)
            }
        }
        return null
    }
}