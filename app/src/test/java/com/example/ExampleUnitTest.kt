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

  @Test
  fun chronologicalNotes_filterAndSortCorrectly() {
    val report1 = com.example.data.IntervalReport(
      id = 1L,
      startDate = 1710460800000L,
      endDate = 1711843200000L,
      startString = "15.03.2024",
      endString = "31.03.2024",
      days = 16,
      normalStart = 1050.0,
      normalEnd = 1100.0,
      normalConsumption = 50.0,
      heatPumpStart = 530.0,
      heatPumpEnd = 560.0,
      heatPumpConsumption = 30.0,
      totalConsumption = 80.0,
      normalWorkCost = 15.0,
      normalBaseCost = 5.0,
      normalTotalCost = 20.0,
      heatPumpWorkCost = 7.5,
      heatPumpBaseCost = 4.0,
      heatPumpTotalCost = 11.5,
      totalCost = 31.5,
      note = "Spätere Notiz am 31.03."
    )

    val report2 = com.example.data.IntervalReport(
      id = 2L,
      startDate = 1709251200000L,
      endDate = 1710460800000L,
      startString = "01.03.2024",
      endString = "15.03.2024",
      days = 14,
      normalStart = 1000.0,
      normalEnd = 1050.0,
      normalConsumption = 50.0,
      heatPumpStart = 500.0,
      heatPumpEnd = 530.0,
      heatPumpConsumption = 30.0,
      totalConsumption = 80.0,
      normalWorkCost = 15.0,
      normalBaseCost = 5.0,
      normalTotalCost = 20.0,
      heatPumpWorkCost = 7.5,
      heatPumpBaseCost = 4.0,
      heatPumpTotalCost = 11.5,
      totalCost = 31.5,
      note = "Frühere Notiz am 15.03."
    )

    val report3 = com.example.data.IntervalReport(
      id = 3L,
      startDate = 1711843200000L,
      endDate = 1712275200000L,
      startString = "31.03.2024",
      endString = "05.04.2024",
      days = 5,
      normalStart = 1100.0,
      normalEnd = 1120.0,
      normalConsumption = 20.0,
      heatPumpStart = 560.0,
      heatPumpEnd = 570.0,
      heatPumpConsumption = 10.0,
      totalConsumption = 30.0,
      normalWorkCost = 6.0,
      normalBaseCost = 1.6,
      normalTotalCost = 7.6,
      heatPumpWorkCost = 2.5,
      heatPumpBaseCost = 1.3,
      heatPumpTotalCost = 3.8,
      totalCost = 11.4,
      note = "" // Leere Notiz
    )

    val intervals = listOf(report1, report2, report3)
    val notesList = intervals
      .filter { it.note.trim().isNotBlank() }
      .sortedBy { it.startDate }

    assertEquals(2, notesList.size)
    assertEquals("Frühere Notiz am 15.03.", notesList[0].note)
    assertEquals("Spätere Notiz am 31.03.", notesList[1].note)
  }
}
