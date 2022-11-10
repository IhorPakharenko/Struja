package com.isao.struja.scrape.cedisAnnouncement

import com.isao.struja.application.db.model.Location
import com.isao.struja.util.levenshtein
import com.isao.struja.util.normalizeSpaces
import it.skrape.core.document
import it.skrape.fetcher.BrowserFetcher
import it.skrape.fetcher.Result
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.DocElement
import it.skrape.selects.html5.div
import java.time.LocalTime
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField

//TODO scrape http://cedis.me/kontakt/

//TODO maybe refactor this into dsl
class CedisAnnouncementScrapper(private val supportedTownNames: List<String>) {
    fun getAnnouncementDetails(url: String) = skrape(BrowserFetcher) {
        request { this.url = url }
        response {
            require(responseStatus.code == 200) {
                //TODO alert the dev
            }

            val elements = selectDaySections().map { daySection ->
                daySection.selectTownSections()
            }
            elements.map { dayElements ->
                dayElements.map { (townsElement, outageTimeAndPlaceElements) ->
                    TownOutageAnnouncement(
                        towns = townsElement.toTownNames(),
                        timeAndPlaces = outageTimeAndPlaceElements.mapNotNull { outageTimeAndPlaceElement ->
                            outageTimeAndPlaceElement.toOutageTimeAndPlaces()
                        }
                    )
                }
            }
        }
    }

    private fun Result.selectDaySections(): List<List<DocElement>> {
        val postContent = document.div {
            withClass = "post-content"
            findFirst { this }
        }
        // Here it is assumed that all scraped elements are on the same level of nesting.
        // If it ever changes, the following logic will break
        return postContent.children.fold(mutableListOf<MutableList<DocElement>>()) { acc, docElement ->
            acc.apply {
                if (levenshtein(docElement.ownText, ANNOUNCEMENT_DAY_OWN_TEXT) < 5) {
                    add(mutableListOf())
                } else {
                    lastOrNull()?.add(docElement)
                }
            }
        }
    }

    private fun List<DocElement>.selectTownSections(): Map<DocElement, List<DocElement>> =
        fold(mutableMapOf<DocElement, MutableList<DocElement>>()) { acc, docElement ->
            acc.apply {
                //TODO this way data from not yet added towns goes to the last known town
                if (docElement.toTownNames().all { supportedTownNames.contains(it) }) {
                    put(docElement, mutableListOf())
                } else {
                    //TODO check if filtering empty elements here doesn't break anything
                    if (docElement.text.isNotEmpty()) {
                        this.values.lastOrNull()?.add(docElement)
                    }
                }
            }
        }

    private fun DocElement.toTownNames() = text.normalizeSpaces().split(*LIST_PARTS_DELIMITERS)

    data class TownOutageAnnouncement(
        val towns: List<String>,
        val timeAndPlaces: List<OutageTimeAndPlaces>
    )

    data class OutageTimeAndPlaces(
        val timeRanges: List<TimeRange>,
        val places: List<String>,
        val outageType: Location.OutageType
    ) {
        data class TimeRange(val startTime: LocalTime, val endTime: LocalTime)
    }

    // Example:
    // – u terminu od 8 do 9 i od 14 do 15 sati: Bašanje brdo, Dom zdravlja, Gorska, Industrijsko, Izbjegličko, SM polje, Vijenac, Vodenica i Željeznička stanica.
    private fun DocElement.toOutageTimeAndPlaces(): OutageTimeAndPlaces? {
        //Consider replacing throwing exceptions with using optionals
        val rawTimeRanges = "(\\d{1,2}:?\\d{0,2})[\\w\\s]{2,4}(\\d{1,2}:?\\d{0,2})".toRegex().find(text)?.groupValues?.drop(1)?.map { it.normalizeSpaces() }?.chunked(2)?: return null

//        val (_, rawTimeRanges, rawPlacesAndExtras) = ANNOUNCEMENT_TIME_AND_PLACES_MATCHER.find(text)?.groupValues
//            ?: return null
//        (\d{1,2}:?\d{0,2})[\w\s]{2,4}(\d{1,2}:?\d{0,2})

        val timeRanges: List<OutageTimeAndPlaces.TimeRange> = rawTimeRanges
            .map { rawTimeRange ->
                val (startTime, endTime) = rawTimeRange
                OutageTimeAndPlaces.TimeRange(
                    LocalTime.parse(startTime, TIME_FORMATTER),
                    LocalTime.parse(endTime, TIME_FORMATTER)
                )
            }
        val rawPlacesAndExtras = "(sati.*)\$".toRegex().find(text)?.groupValues?.last()?.normalizeSpaces()?: return null

        //TODO refactor
        val maybeExtraStartIndex = rawPlacesAndExtras.lastIndexOfAny(charArrayOf('(', '–'))
        val maybeExtras = if (maybeExtraStartIndex != -1) {
            rawPlacesAndExtras.substring(maybeExtraStartIndex)
        } else {
            rawPlacesAndExtras
        }

        val outageType = parseOutageType(maybeExtras)
        val rawPlaces = if (outageType != null) {
            rawPlacesAndExtras.replace(maybeExtras, "")
        } else {
            rawPlacesAndExtras
        }

        val places = rawPlaces.replace("[:–]".toRegex(), "").split(*LIST_PARTS_DELIMITERS)

        return OutageTimeAndPlaces(
            timeRanges = timeRanges,
            places = places,
            outageType = outageType ?: Location.OutageType.LASTING_OUTAGE
        )
    }

    private fun parseOutageType(string: String): Location.OutageType? {
        if (!string.contains("isključenja")) return null
        return if (string.contains("kratkotrajna") || string.contains("kratka")) {
            Location.OutageType.SHORT_TERM_OUTAGES
        } else {
            Location.OutageType.LASTING_OUTAGE
        }
    }

    companion object {
        const val ANNOUNCEMENT_DAY_OWN_TEXT =
            "Zbog planiranih radova na mreži, , bez napajanja električnom energijom će ostati:"

        //TODO shrunk the matcher as much as possible to handle at least some of their misprints. Check if this doesn't break anything
//        val ANNOUNCEMENT_TIME_AND_PLACES_MATCHER = "u terminu od\\s*(.*)sati\\W*(.*)".toRegex()
        val ANNOUNCEMENT_TIME_AND_PLACES_MATCHER = "od\\s*(.*)sati\\W*(.*)".toRegex()
        val LIST_PARTS_DELIMITERS = arrayOf("/", ",", " i ")
        val TIME_FORMATTER = DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
            .optionalStart()
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE)
            .toFormatter()
    }
}