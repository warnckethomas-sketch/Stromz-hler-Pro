package com.example.data

import kotlin.math.max

data class IntervalReport(
    val id: Long,
    val startDate: Long,
    val endDate: Long,
    val startString: String,
    val endString: String,
    val days: Int,
    val normalStart: Double,
    val normalEnd: Double,
    val normalConsumption: Double,
    val heatPumpStart: Double,
    val heatPumpEnd: Double,
    val heatPumpConsumption: Double,
    val totalConsumption: Double,
    val normalWorkCost: Double,
    val normalBaseCost: Double,
    val normalTotalCost: Double,
    val heatPumpWorkCost: Double,
    val heatPumpBaseCost: Double,
    val heatPumpTotalCost: Double,
    val totalCost: Double,
    val note: String
)

fun calculateIntervals(
    readings: List<MeterReadingEntity>,
    tariffs: TariffSettingsEntity
): List<IntervalReport> {
    if (readings.size < 2) return emptyList()

    val sorted = readings.sortedBy { it.date }
    val reports = mutableListOf<IntervalReport>()

    for (i in 1 until sorted.size) {
        val prev = sorted[i - 1]
        val curr = sorted[i]

        val diffMs = curr.date - prev.date
        val days = max(1, (diffMs / (1000 * 60 * 60 * 24)).toInt())

        val normalCons = max(0.0, curr.normalReading - prev.normalReading)
        val hpCons = max(0.0, curr.heatPumpReading - prev.heatPumpReading)
        val totalCons = normalCons + hpCons

        val monthsFraction = days / 30.4375

        // Costs
        val normalWork = (normalCons * tariffs.normalWorkPriceCt) / 100.0
        val normalBase = tariffs.normalBaseFeeEurMonth * monthsFraction
        val normalTotal = normalWork + normalBase

        val hpWork = (hpCons * tariffs.heatPumpWorkPriceCt) / 100.0
        val hpBase = tariffs.heatPumpBaseFeeEurMonth * monthsFraction
        val hpTotal = hpWork + hpBase

        val totalCost = normalTotal + hpTotal

        val noteText = when {
            curr.note.isNotBlank() && prev.note.isNotBlank() && curr.note != prev.note ->
                "${curr.note} (${com.example.utils.DateUtils.formatToGermanDate(prev.dateString)}: ${prev.note})"
            curr.note.isNotBlank() -> curr.note
            prev.note.isNotBlank() -> prev.note
            else -> ""
        }

        reports.add(
            IntervalReport(
                id = curr.id,
                startDate = prev.date,
                endDate = curr.date,
                startString = com.example.utils.DateUtils.formatToGermanDate(prev.dateString),
                endString = com.example.utils.DateUtils.formatToGermanDate(curr.dateString),
                days = days,
                normalStart = prev.normalReading,
                normalEnd = curr.normalReading,
                normalConsumption = normalCons,
                heatPumpStart = prev.heatPumpReading,
                heatPumpEnd = curr.heatPumpReading,
                heatPumpConsumption = hpCons,
                totalConsumption = totalCons,
                normalWorkCost = normalWork,
                normalBaseCost = normalBase,
                normalTotalCost = normalTotal,
                heatPumpWorkCost = hpWork,
                heatPumpBaseCost = hpBase,
                heatPumpTotalCost = hpTotal,
                totalCost = totalCost,
                note = noteText
            )
        )
    }

    return reports.sortedByDescending { it.endDate }
}
