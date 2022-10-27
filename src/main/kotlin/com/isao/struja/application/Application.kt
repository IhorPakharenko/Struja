package com.isao.struja.application

import com.isao.struja.scrape.CedisAnnouncementTitleScrapper
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty) {
        launch {
            CedisAnnouncementTitleScrapper().getLatestAnnouncementTitles().map { title ->
                println("${title.title}:${title.dates.map { it.toString() }.joinToString { it }}")
            }
//            while (true) {
//                println("Scraping")
//                delay(5.seconds)
//            }
        }
        routing {
            get("/") {
                call.respondText("Hello, world!")
            }
        }
    }.start(wait = true)
}