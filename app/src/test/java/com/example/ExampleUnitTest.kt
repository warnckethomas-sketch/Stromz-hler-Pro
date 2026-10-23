package com.example

import com.example.data.MeterReadingEntity
import com.example.data.TariffSettingsEntity
import com.example.data.calculateIntervals
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun calculateIntervals_onlySetsNoteIfCurrentIntervalHasNote() {
    val tariffs = TariffSettingsEntity(
      normalWorkPriceCt = 30.0,
      normalBaseFeeEurMonth = 10.0,
      heatPumpWorkPriceCt = 25.0,
      heatPumpBaseFeeEurMonth = 8.0
    )

    val reading1 = MeterReadingEntity(
      id = 1L,
      date = 1704067200000L, // 2024-01-01
      dateString = "2024-01-01",
      normalReading = 1000.0,
      heatPumpReading = 500.0,
      tenantId = 1L,
      note = "Startzählerstand"
    )

    val reading2 = MeterReadingEntity(
      id = 2L,
      date = 1706745600000L, // 2024-02-01
      dateString = "2024-02-01",
      normalReading = 1100.0,
      heatPumpReading = 600.0,
      tenantId = 1L,
      note = "" // Keine Notiz für aktuelles Intervall
    )

    val reading3 = MeterReadingEntity(
      id = 3L,
      date = 1709251200000L, // 2024-03-01
      dateString = "2024-03-01",
      normalReading = 1200.0,
      heatPumpReading = 700.0,
      tenantId = 1L,
      note = "Zählerwechsel im März" // Notiz für dieses Intervall
    )

    val intervals = calculateIntervals(listOf(reading1, reading2, reading3), tariffs)
    assertEquals(2, intervals.size)

    // Sorted descending by endDate: intervals[0] is reading2->reading3, intervals[1] is reading1->reading2
    val intervalMarch = intervals.first { it.endString.contains("01.03.2024") || it.endString.contains("2024-03-01") }
    val intervalFeb = intervals.first { it.endString.contains("01.02.2024") || it.endString.contains("2024-02-01") }

    assertEquals("Zählerwechsel im März", intervalMarch.note)
    assertEquals("", intervalFeb.note)
  }
}
