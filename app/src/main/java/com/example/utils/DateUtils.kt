package com.example.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    val GERMAN_DATE_FORMAT: SimpleDateFormat
        get() = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)

    fun todayGerman(): String {
        return GERMAN_DATE_FORMAT.format(Date())
    }

    fun formatToGermanDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        val trimmed = dateStr.trim()
        // Already dd.MM.yyyy
        if (trimmed.matches(Regex("""\d{2}\.\d{2}\.\d{4}"""))) {
            return trimmed
        }
        // yyyy-MM-dd
        if (trimmed.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
            val parts = trimmed.split("-")
            return "${parts[2]}.${parts[1]}.${parts[0]}"
        }
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN).parse(trimmed)
            if (parsed != null) GERMAN_DATE_FORMAT.format(parsed) else trimmed
        } catch (e: Exception) {
            trimmed
        }
    }
}
