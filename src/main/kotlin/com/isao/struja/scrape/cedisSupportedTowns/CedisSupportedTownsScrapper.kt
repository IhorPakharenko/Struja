package com.isao.struja.scrape.cedisSupportedTowns

import com.isao.struja.scrape.cedisAnnouncement.CedisAnnouncementScrapper
import com.isao.struja.util.normalizeSpaces
import it.skrape.core.document
import it.skrape.fetcher.BrowserFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.eachText
import it.skrape.selects.html5.table
import it.skrape.selects.html5.td
import it.skrape.selects.html5.tr

class CedisSupportedTownsScrapper {
    fun getSupportedTowns(url: String) = skrape(BrowserFetcher) {
        request { this.url = url }
        response {
            require(responseStatus.code == 200) {
                //TODO alert the dev
            }

            document
                .table {
                    tr {
                        td {
                            findAll { this }
                        }
                    }
                }
                .filter { it.children.isEmpty() } // Filter out first rows with text in <strong> that contain no useful data
                .filter { it.className.isEmpty() && it.text.isNotEmpty() } // Filter out second and third columns
                .flatMap { it.text.split(*LIST_PARTS_DELIMITERS) }
                .map { it.normalizeSpaces() }
        }
    }

    companion object {
        //TODO it is a copypaste from another class. That's disgusting
        val LIST_PARTS_DELIMITERS = arrayOf("/", ",", " i ")
    }
}