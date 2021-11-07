package io.github.omar.config

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.hocon

object Config {
    object Ktor : ConfigSpec() {

        object Deployment : ConfigSpec() {
            val port by required<Int>()
            val host by optional("0.0.0.0")
        }

        object ESPN : ConfigSpec() {
            val espnS2 by required<String>()
            val swid by required<String>()
            val leagueID by required<Int>()
        }

        object Telegram : ConfigSpec() {
            val chatID by required<Long>()
            val token by required<String>()
            val timezone by optional("America/Chicago")
        }
    }

    fun loadConfig(): Config {
        val topLevelConfigs = arrayOf(
            Ktor
        )
        val env = System.getenv("ENV") ?: "dev"

        val conf = Config {
            topLevelConfigs.forEach { addSpec(it) }
        }.from.hocon.resource("application.conf")

        return when (env) {
            "dev" -> conf
            "stage" -> conf.from.hocon.resource("application-stage.conf")
            "prod" -> conf.from.hocon.resource("application-prod.conf")
            else -> throw IllegalArgumentException("Unsupported environment: $env")
        }.from.env()
    }
}