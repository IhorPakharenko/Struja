package com.isao.struja.application

import com.isao.struja.scrape.cedisAnnouncement.CedisAnnouncementScrapper
import com.isao.struja.scrape.cedisAnnouncementTitle.CedisAnnouncementTitleScrapper
import com.isao.struja.scrape.cedisSupportedTowns.CedisSupportedTownsScrapper
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch

fun main() {
    embeddedServer(Netty) {
        launch {
            //TODO use Eithers by arrow-core
            val supportedTowns = CedisSupportedTownsScrapper().getSupportedTowns("http://cedis.me/kontakt/")
            CedisAnnouncementTitleScrapper().getLatestAnnouncementTitles().map { title ->
//                println("${title.title}:${title.dates.map { it.toString() }.joinToString { it }}")
                val result = CedisAnnouncementScrapper(
                    supportedTowns
//                    listOf("Podgorica", "Danilovgrad", "Cetinje", "Herceg Novi")
                ).getAnnouncementDetails(title.url)
                result
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