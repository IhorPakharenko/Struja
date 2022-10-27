package com.isao.struja.application.db.model

data class Town(
    val name: String,
    val locations: Set<Location>
)