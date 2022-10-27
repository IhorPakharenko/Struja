package com.isao.struja.util

fun String.normalizeSpaces() = trim().replace("\\s{2,}".toRegex(), " ")