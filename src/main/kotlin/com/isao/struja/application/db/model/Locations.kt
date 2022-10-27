package com.isao.struja.application.db.model

import java.time.Period

data class Location(
    val name: String,
    val outagePeriod: Period,
    val outageType: OutageType
) {
    enum class OutageType { LASTING_OUTAGE, SHORT_TERM_OUTAGES }
}