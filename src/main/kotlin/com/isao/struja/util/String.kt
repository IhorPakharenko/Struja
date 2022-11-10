package com.isao.struja.util

fun String.normalizeSpaces() = trim().replace("\\s{2,}".toRegex(), " ")

fun String.diff(another: String) = levenshtein(this, another)

fun String.remove(other: String, ignoreCase: Boolean = false) = replace(other, "", ignoreCase)
fun String.remove(other: Char, ignoreCase: Boolean = false) = replace(other.toString(), "", ignoreCase)
fun String.remove(regex: Regex) = replace(regex, "")

operator fun String.minus(other: String) = remove(other)
operator fun String.minus(other: Char) = remove(other)