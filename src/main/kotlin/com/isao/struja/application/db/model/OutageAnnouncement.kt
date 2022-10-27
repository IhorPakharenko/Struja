package com.isao.struja.application.db.model

import java.time.LocalDate

data class OutageAnnouncement(
    val id: String,
    val date: LocalDate,
    val towns: Set<Town>,
    val url: String
)