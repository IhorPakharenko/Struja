package com.isao.struja.scrape

import com.isao.struja.scrape.CedisAnnouncementTitleScrapper.Companion.DATES_AND_MONTH_MATCHER
import com.isao.struja.scrape.CedisAnnouncementTitleScrapper.Companion.MONTH_MATCHER
import com.isao.struja.util.normalizeSpaces
import it.skrape.core.htmlDocument
import it.skrape.fetcher.*
import java.time.LocalDate
import it.skrape.selects.DocElement
import it.skrape.selects.html5.a
import it.skrape.selects.html5.div
import it.skrape.selects.html5.h3

class CedisAnnouncementTitleScrapper {
    //TODO use faster HttpFetcher. For some reason right now it fails with NoClassDefFoundError NativeUtilsJvmKt
    fun getLatestAnnouncementTitles(): List<AnnouncementTitle> = skrape(BrowserFetcher) {
        request { url = "http://cedis.me/?post_type=exhibition" }
        response {
            require(responseStatus.code == 200) {
                //TODO alert the dev
            }
            selectAllTitleElements().map {
                it.toAnnouncementTitle()
            }
        }
    }.filterActualAnnouncements()

    data class AnnouncementTitle(val title: String, val url: String, val dates: List<LocalDate>)

    companion object {
        val MONTHS_IN_SOURCE_LANGUAGE = listOf(
            "januar",
            "februar",
            "mart",
            "april",
            "maj",
            "jun",
            "jul",
            "avgust",
            "septembar",
            "oktobar",
            "novembar",
            "decembar"
        )

        private val RAW_MONTH_MATCHER =
            MONTHS_IN_SOURCE_LANGUAGE.joinToString(separator = "|", prefix = "(", postfix = ")")

        // Matches only month names
        val MONTH_MATCHER = RAW_MONTH_MATCHER.toRegex()

        // Matches month names and
        val DATES_AND_MONTH_MATCHER = ".+?$RAW_MONTH_MATCHER".toRegex()
    }
}

//TODO better name
fun List<CedisAnnouncementTitleScrapper.AnnouncementTitle>.filterActualAnnouncements(): List<CedisAnnouncementTitleScrapper.AnnouncementTitle> {
    val now = LocalDate.now()
    return this
        .map { announcementTitle ->
            announcementTitle.copy(dates = announcementTitle.dates.filter { it >= now })
        }
        .filter { announcementTitle -> announcementTitle.dates.isNotEmpty() }
}

private fun Result.selectAllTitleElements(): List<DocElement> = htmlDocument {
    div {
        withClass = "posts-listing"
        h3 {
            a {
                findAll {
                    return@findAll this
                }
            }
        }
    }
}

private fun DocElement.toAnnouncementTitle(): CedisAnnouncementTitleScrapper.AnnouncementTitle {
    val (title, url) = findFirst { eachLink }.entries.first()

    val normalizedTitle = title.lowercase().normalizeSpaces()
    val datesAndMonthIndices = DATES_AND_MONTH_MATCHER.findAll(normalizedTitle).toList().map { it.range }.also {
        require(it.isNotEmpty()) {
            //TODO alert the dev
        }
    }
    val datesAndMonthStrings = datesAndMonthIndices.map { text.substring(it) }

    val dates = datesAndMonthStrings.flatMap { datesAndMonthString ->
        val monthString = MONTH_MATCHER.find(datesAndMonthString)?.value
            ?: throw IllegalArgumentException("Month not found in datesAndMonthString")

        // Java.time library enumerates months starting from 1
        val monthIndex = (CedisAnnouncementTitleScrapper.MONTHS_IN_SOURCE_LANGUAGE.indexOf(monthString) + 1).also {
            require(it != 0) {
                //TODO alert the dev
            }
        }

        // Servisne informacije za 16., 17. i 18.oktobar
        // Servisne informacije za 27. oktobar

        val daysOfMonth = "\\d+".toRegex().findAll(datesAndMonthString).toList().map { it.value.toInt() }

        daysOfMonth.map { dayOfMonth ->
            //TODO handle cases of launching the app right after the new year has started
            LocalDate.of(LocalDate.now().year, monthIndex, dayOfMonth)
        }
    }

    return CedisAnnouncementTitleScrapper.AnnouncementTitle(
        title = title,
        url = url,
        dates = dates
    )
}