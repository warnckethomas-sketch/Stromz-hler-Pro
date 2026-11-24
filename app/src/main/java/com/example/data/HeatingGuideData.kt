package com.example.data

enum class HeatingMode {
    ECO,
    COMFORT,
    FROST_PROTECTION
}

data class HeatingTimeSlot(
    val startHour: Float,
    val endHour: Float,
    val timeLabel: String,
    val modeName: String,
    val temperature: String,
    val displaySymbol: String,
    val mode: HeatingMode
)

enum class OgProgramCategory(val label: String) {
    ALL("Alle"),
    MORNING_EVENING("Morgens & Abends"),
    DAYTIME("Tagsüber"),
    SPECIAL("Spezial")
}

data class OgHeatingProgram(
    val number: Int,
    val title: String,
    val summary: String,
    val activeRanges: List<Pair<Float, Float>>,
    val mode: HeatingMode,
    val category: OgProgramCategory
)

object HeatingGuideData {

    val acalorSchedule: List<HeatingTimeSlot> = listOf(
        HeatingTimeSlot(
            startHour = 0f,
            endHour = 6f,
            timeLabel = "00:00 – 06:00",
            modeName = "ECO-Modus",
            temperature = "17 °C",
            displaySymbol = "Leeres Feld (Minus / Mond)",
            mode = HeatingMode.ECO
        ),
        HeatingTimeSlot(
            startHour = 6f,
            endHour = 9f,
            timeLabel = "06:00 – 09:00",
            modeName = "Komfort-Modus",
            temperature = "21 °C",
            displaySymbol = "Sichtbarer Balken (Plus / Sonne)",
            mode = HeatingMode.COMFORT
        ),
        HeatingTimeSlot(
            startHour = 9f,
            endHour = 16f,
            timeLabel = "09:00 – 16:00",
            modeName = "ECO-Modus",
            temperature = "17 °C",
            displaySymbol = "Leeres Feld (Minus / Mond)",
            mode = HeatingMode.ECO
        ),
        HeatingTimeSlot(
            startHour = 16f,
            endHour = 22f,
            timeLabel = "16:00 – 22:00",
            modeName = "Komfort-Modus",
            temperature = "21 °C",
            displaySymbol = "Sichtbarer Balken (Plus / Sonne)",
            mode = HeatingMode.COMFORT
        ),
        HeatingTimeSlot(
            startHour = 22f,
            endHour = 24f,
            timeLabel = "22:00 – 24:00",
            modeName = "ECO-Modus",
            temperature = "17 °C",
            displaySymbol = "Leeres Feld (Minus / Mond)",
            mode = HeatingMode.ECO
        )
    )

    val ogPrograms: List<OgHeatingProgram> = listOf(
        OgHeatingProgram(
            number = 0,
            title = "Programm 0",
            summary = "24 Std. Frostschutz (5 °C)",
            activeRanges = emptyList(),
            mode = HeatingMode.FROST_PROTECTION,
            category = OgProgramCategory.SPECIAL
        ),
        OgHeatingProgram(
            number = 1,
            title = "Programm 1",
            summary = "Heizen 06:00–22:30 (18,5 °C), sonst abgesenkt (14 °C)",
            activeRanges = listOf(6f to 22.5f),
            mode = HeatingMode.COMFORT,
            category = OgProgramCategory.DAYTIME
        ),
        OgHeatingProgram(
            number = 2,
            title = "Programm 2",
            summary = "Heizen 06:00–08:00 + 16:00–22:30",
            activeRanges = listOf(6f to 8f, 16f to 22.5f),
            mode = HeatingMode.COMFORT,
            category = OgProgramCategory.MORNING_EVENING
        ),
        OgHeatingProgram(
            number = 3,
            title = "Programm 3",
            summary = "Heizen 06:00–07:30 + 10:30–12:30 + 16:00–22:30",
            activeRanges = listOf(6f to 7.5f, 10.5f to 12.5f, 16f to 22.5f),
            mode = HeatingMode.COMFORT,
            category = OgProgramCategory.MORNING_EVENING
        ),
        OgHeatingProgram(
            number = 4,
            title = "Programm 4",
            summary = "Heizen 08:00–15:00",
            activeRanges = listOf(8f to 15f),
            mode = HeatingMode.COMFORT,
            category = OgProgramCategory.DAYTIME
        ),
        OgHeatingProgram(
            number = 5,
            title = "Programm 5",
            summary = "Heizen 08:00–11:00 + 13:00–16:00",
            activeRanges = listOf(8f to 11f, 13f to 16f),
            mode = HeatingMode.COMFORT,
            category = OgProgramCategory.DAYTIME
        ),
        OgHeatingProgram(
            number = 6,
            title = "Programm 6",
            summary = "Heizen 08:00–13:00",
            activeRanges = listOf(8f to 13f),
            mode = HeatingMode.COMFORT,
            category = OgProgramCategory.DAYTIME
        ),
        OgHeatingProgram(
            number = 7,
            title = "Programm 7",
            summary = "Heizen 12:00–22:30",
            activeRanges = listOf(12f to 22.5f),
            mode = HeatingMode.COMFORT,
            category = OgProgramCategory.DAYTIME
        ),
        OgHeatingProgram(
            number = 8,
            title = "Programm 8",
            summary = "Heizen 24 Std. (18,5 °C)",
            activeRanges = listOf(0f to 24f),
            mode = HeatingMode.COMFORT,
            category = OgProgramCategory.SPECIAL
        ),
        OgHeatingProgram(
            number = 9,
            title = "Programm 9",
            summary = "Abgesenkt 24 Std. (14 °C)",
            activeRanges = emptyList(),
            mode = HeatingMode.ECO,
            category = OgProgramCategory.SPECIAL
        ),
        OgHeatingProgram(
            number = 10,
            title = "Programm 10",
            summary = "Heizen 05:00–10:00 + 12:00–14:00 + 17:00–22:00",
            activeRanges = listOf(5f to 10f, 12f to 14f, 17f to 22f),
            mode = HeatingMode.COMFORT,
            category = OgProgramCategory.MORNING_EVENING
        ),
        OgHeatingProgram(
            number = 11,
            title = "Programm 11",
            summary = "Heizen 05:00–07:00 + 16:00–22:00",
            activeRanges = listOf(5f to 7f, 16f to 22f),
            mode = HeatingMode.COMFORT,
            category = OgProgramCategory.MORNING_EVENING
        )
    )

    val WEEKDAYS: List<String> = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

    const val DEFAULT_EG_MASK = "000000111000000011111100"

    fun parseEgMask(mask: String): List<Boolean> {
        return List(24) { idx ->
            mask.getOrNull(idx) == '1'
        }
    }

    fun encodeEgMask(hours: List<Boolean>): String {
        return (0 until 24).joinToString("") { idx ->
            if (hours.getOrNull(idx) == true) "1" else "0"
        }
    }

    fun parseWeeklyMasks(raw: String): List<List<Boolean>> {
        val parts = if (raw.isBlank()) emptyList() else raw.split("|")
        return List(7) { dayIdx ->
            parseEgMask(parts.getOrNull(dayIdx) ?: DEFAULT_EG_MASK)
        }
    }

    fun encodeWeeklyMasks(days: List<List<Boolean>>): String {
        return (0 until 7).joinToString("|") { dayIdx ->
            encodeEgMask(days.getOrNull(dayIdx) ?: parseEgMask(DEFAULT_EG_MASK))
        }
    }

    fun parseOgDayPrograms(raw: String): List<Int> {
        val parts = if (raw.isBlank()) emptyList() else raw.split(",")
        return List(7) { dayIdx ->
            (parts.getOrNull(dayIdx)?.trim()?.toIntOrNull() ?: 1).coerceIn(0, 11)
        }
    }

    fun encodeOgDayPrograms(programs: List<Int>): String {
        return (0 until 7).joinToString(",") { dayIdx ->
            (programs.getOrNull(dayIdx) ?: 1).coerceIn(0, 11).toString()
        }
    }

    fun formatHourRanges(hours: List<Boolean>): String {
        val ranges = mutableListOf<String>()
        var start: Int? = null
        for (h in 0 until 24) {
            val isComfort = hours.getOrNull(h) == true
            if (isComfort && start == null) {
                start = h
            } else if (!isComfort && start != null) {
                ranges.add(String.format("%02d–%02d", start, h))
                start = null
            }
        }
        if (start != null) {
            ranges.add(String.format("%02d–24", start))
        }
        return when {
            ranges.isEmpty() -> "Durchgehend ECO"
            ranges.size == 1 && ranges[0] == "00–24" -> "Durchgehend Komfort"
            else -> "${ranges.joinToString(", ")} Uhr Komfort"
        }
    }

    private fun formatTemp(temp: Double): String {
        return if (temp % 1.0 == 0.0) {
            String.format(java.util.Locale.GERMAN, "%.0f °C", temp)
        } else {
            String.format(java.util.Locale.GERMAN, "%.1f °C", temp)
        }
    }

    fun formatOgScheduleSummary(programs: List<Int>): String {
        val distinct = programs.distinct()
        if (distinct.size == 1) {
            return "Mo–So: P${distinct[0]}"
        }
        val weekdaysSame = programs.take(5).distinct().size == 1
        val weekendSame = programs.drop(5).distinct().size == 1
        if (weekdaysSame && weekendSame) {
            return "Mo–Fr: P${programs[0]}, Sa–So: P${programs[5]}"
        }
        return WEEKDAYS.mapIndexed { idx, day -> "$day: P${programs.getOrNull(idx) ?: 1}" }.joinToString(", ")
    }

    fun formatEgScheduleSummary(settings: HeatingSettingsEntity): String {
        return if (settings.egScheduleType == "WEEKLY") {
            val weekly = parseWeeklyMasks(settings.egWeeklyMasks)
            val descriptions = weekly.map { formatHourRanges(it) }
            if (descriptions.distinct().size == 1) {
                "Mo–So: ${descriptions[0]}"
            } else if (descriptions.take(5).distinct().size == 1 && descriptions.drop(5).distinct().size == 1) {
                "Mo–Fr: ${descriptions[0]}; Sa–So: ${descriptions[5]}"
            } else {
                WEEKDAYS.mapIndexed { idx, day -> "$day: ${descriptions[idx]}" }.joinToString("; ")
            }
        } else {
            "All: ${formatHourRanges(parseEgMask(settings.egAllDaysMask))}"
        }
    }

    fun formatHeatingExtraNote(settings: HeatingSettingsEntity): String {
        val dateStr = com.example.utils.DateUtils.formatToGermanDate(settings.effectiveDateString)
        val egModeText = if (settings.egMode == "KUEHLEN") {
            "Kühlen (${formatTemp(settings.egCoolTemp)})"
        } else {
            "Heizen (Komfort ${formatTemp(settings.egComfortTemp)} / ECO ${formatTemp(settings.egEcoTemp)})"
        }
        val egSched = formatEgScheduleSummary(settings)
        val ogProgs = parseOgDayPrograms(settings.ogDayPrograms)
        val ogSched = formatOgScheduleSummary(ogProgs)
        val ogTemps = "Heizen ${formatTemp(settings.ogHeatTemp)} / Abgesenkt ${formatTemp(settings.ogSetbackTemp)}"
        return "Heizung ($dateStr):\n• EG: $egModeText, $egSched\n• OG: $ogTemps ($ogSched)"
    }
}
