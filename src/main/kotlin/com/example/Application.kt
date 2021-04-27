package com.example

import com.example.config.AppConfig
import com.example.config.getDLS
import com.example.config.initDb
import com.example.dao.UserDao
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.example.controller.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.serialization.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json()
        }
        val appConfig = AppConfig()
        initDb(appConfig)
        val userDao = UserDao(getDLS(appConfig))
        configureRouting(userDao)
    }.start(wait = true)
}
