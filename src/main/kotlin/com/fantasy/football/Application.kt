package com.fantasy.football

import com.fantasy.football.plugins.configureMonitoring
import com.fantasy.football.plugins.configureRouting
import com.fantasy.football.service.BotService
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

suspend fun main() {
    val botService = BotService()
    embeddedServer(Netty, port = BotService.config.ktor.port.toInt(), host = "0.0.0.0") {
        configureMonitoring()
        configureRouting()
        GlobalScope.launch(Dispatchers.IO) {
            botService.startBot()
        }
    }.start(wait = true)
}
