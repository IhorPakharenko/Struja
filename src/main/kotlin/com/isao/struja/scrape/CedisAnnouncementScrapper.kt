package com.isao.struja.scrape

import com.isao.struja.util.normalizeSpaces
import it.skrape.core.document
import it.skrape.core.htmlDocument
import it.skrape.fetcher.BrowserFetcher
import it.skrape.fetcher.Result
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.DocElement
import it.skrape.selects.html5.a
import it.skrape.selects.html5.div
import it.skrape.selects.html5.h3
import it.skrape.selects.html5.p

//TODO scrape http://cedis.me/kontakt/
class CedisAnnouncementScrapper(val towns: List<String>) {
    private fun Result.selectAnnouncementDayParagraphs(): List<DocElement> = htmlDocument {
        div {
            withClass = "poss-listing"
            h3 {
                a {
                    findAll {
                        return@findAll this
                    }
                }
            }
        }
    }

    fun getAnnouncementDetails(url: String) = skrape(BrowserFetcher) {
        request { this.url = url }
        response {
            require(responseStatus.code == 200) {
                //TODO alert the dev
            }

            val postContent = document.div {
                withClass = "post-content"
                findFirst { this }
            }
            val postContentChildren = postContent.children

            val announcementDayParagraphs = postContent.p {
                findAll {
                    filterNot {
                        ANNOUNCEMENT_DAY_MATCHER.find(it.text.normalizeSpaces())?.value.isNullOrEmpty()
                    }
                }
            }
            require(announcementDayParagraphs.isNotEmpty()) {
                //TODO alert the dev
            }

            val dayParagraphIndices = announcementDayParagraphs.map {dayParagraph ->
                postContentChildren.indexOfFirst { it.text == dayParagraph.text }
            }
            val dayParagraphRanges = (dayParagraphIndices + postContentChildren.lastIndex).zipWithNext { first, second ->
                first..second
            }

            val daySections: List<List<DocElement>> = dayParagraphRanges.map { range ->
                postContentChildren.subList(range.first, range.last + 1)
            }

            daySections.forEach { daySection ->
                //TODO handle "Podgorica/Tuzi"
                val townParagraphs: List<DocElement> = daySection.filter { paragraph ->
                    TOWNS.contains(paragraph.text.normalizeSpaces())
                }
                val townParagraphIndicies = townParagraphs.withIndex().map { it.index }
                require(townParagraphs.isNotEmpty()) {
                    //TODO alert the dev
                }

                val townParagraphRanges = (townParagraphIndicies + townParagraphs.lastIndex).zipWithNext { first, second ->
                    first..second
                }

                //TODO gods forgive me. Rewrite
                val townParagraphsFinal: List<List<DocElement>> = townParagraphRanges.map {range ->
                    townParagraphs.subList(range.first, range.last + 1)
                }

                townParagraphsFinal
            }

            println(announcementDayParagraphs.size)
        }
    }

    companion object {
        val ANNOUNCEMENT_DAY_MATCHER = "(?<=Zbog planiranih radova na mreži).*(?=bez napajanja električnom energijom će ostati)".toRegex(RegexOption.IGNORE_CASE)
        val SHORT_TERM_OUTAGES_TEXT = "kratkotrajna isključenja u navedenom terminu"
        val POSSIBLE_OUTAGES_TEXT = "moguća isključenja u navedenom periodu"
        val ANNOUNCEMENT_TIME_AND_PLACE_MATCHER = "u terminu od\\s*(.*)sati\\W*(.*)($SHORT_TERM_OUTAGES_TEXT|$POSSIBLE_OUTAGES_TEXT)".toRegex()
        val TOWNS = listOf("Podgorica", "Danilovgrad", "Cetinje", "Herceg Novi")
    }
}