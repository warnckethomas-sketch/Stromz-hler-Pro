package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.IntervalReport
import com.example.data.PropertySettingsEntity
import com.example.data.TariffSettingsEntity
import com.example.data.TenantEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun generateIntervalReportPdf(
        context: Context,
        report: IntervalReport,
        tariffs: TariffSettingsEntity,
        property: PropertySettingsEntity?,
        tenant: TenantEntity?
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size at 72 DPI
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
        }

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 18f
            isFakeBoldText = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 12f
            isFakeBoldText = true
        }

        val headerPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 11f
            isFakeBoldText = true
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }

        var y = 45f
        val leftMargin = 40f
        val rightMargin = 555f

        // Title
        canvas.drawText("Strom- & Wärmepumpen-Zählerbericht", leftMargin, y, titlePaint)
        y += 18f
        val df = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
        val genDateStr = df.format(Date())
        canvas.drawText("Erstellt am: $genDateStr", leftMargin, y, paint)
        y += 36f // Two blank lines under "Erstellt am"

        // Mietobjekt
        if (property != null) {
            canvas.drawText("Mietobjekt: ${property.propertyName}", leftMargin, y, subtitlePaint)
            y += 26f // Leerzeile nach Mietobjekt
            if (property.propertyAddress.isNotBlank()) {
                canvas.drawText("Anschrift: ${property.propertyAddress}", leftMargin, y, paint)
                y += 26f // Leerzeile nach Anschrift
            }
            if (property.meterNumberNormal.isNotBlank() || property.meterNumberHeatPump.isNotBlank()) {
                val meterLabelX = leftMargin
                val meterValueX = leftMargin + 160f
                canvas.drawText("Zähler-Nr. Tag-Strom:", meterLabelX, y, paint)
                canvas.drawText(property.meterNumberNormal.ifBlank { "-" }, meterValueX, y, paint)
                y += 15f
                canvas.drawText("Zähler-Nr. Wärmepumpe:", meterLabelX, y, paint)
                canvas.drawText(property.meterNumberHeatPump.ifBlank { "-" }, meterValueX, y, paint)
                y += 18f
            }
        }

        // Trennlinie
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 20f

        // Mieter
        if (tenant != null) {
            canvas.drawText("Mieter: ${tenant.name}", leftMargin, y, subtitlePaint)
            y += 15f
            if (tenant.address.isNotBlank()) {
                canvas.drawText("Anschrift: ${tenant.address}", leftMargin, y, paint)
                y += 15f
            }
            if (tenant.phone.isNotBlank()) {
                canvas.drawText("Telefon: ${tenant.phone}", leftMargin, y, paint)
                y += 15f
            }
            val startStr = com.example.utils.DateUtils.formatToGermanDate(tenant.startDate)
            val endStr = if (tenant.endDate.isNotBlank()) " (Auszug: ${com.example.utils.DateUtils.formatToGermanDate(tenant.endDate)})" else ""
            canvas.drawText("Einzugsdatum: $startStr$endStr", leftMargin, y, paint)
            y += 22f
        }

        // Trennlinie
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 20f

        // Interval Info
        canvas.drawText("Ablesezeitraum:", leftMargin, y, headerPaint)
        y += 16f
        val repStart = com.example.utils.DateUtils.formatToGermanDate(report.startString)
        val repEnd = com.example.utils.DateUtils.formatToGermanDate(report.endString)
        canvas.drawText("Von: $repStart  bis  $repEnd (${report.days} Tage)", leftMargin, y, paint)

        if (report.note.isNotBlank()) {
            y += 26f // Eine Leerzeile zwischen Zeitraum und Notiz
            val notePaint = Paint(paint).apply { color = Color.parseColor("#334155") }
            val noteLines = report.note.split("\n")
            noteLines.forEachIndexed { index, line ->
                val prefix = if (index == 0) "Notiz: " else "       "
                canvas.drawText("$prefix$line", leftMargin, y, notePaint)
                y += 14f
            }
            y += 12f
        } else {
            y += 24f
        }

        // Meter Readings Table Header
        canvas.drawText("Zählerstände & Verbrauch", leftMargin, y, headerPaint)
        y += 16f

        canvas.drawText("Zähler", leftMargin, y, subtitlePaint)
        canvas.drawText("Alt", leftMargin + 150f, y, subtitlePaint)
        canvas.drawText("Neu", leftMargin + 250f, y, subtitlePaint)
        canvas.drawText("Verbrauch", leftMargin + 350f, y, subtitlePaint)
        y += 6f
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 18f

        // Tag-Strom row
        canvas.drawText("Tag-Strom", leftMargin, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.1f kWh", report.normalStart), leftMargin + 150f, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.1f kWh", report.normalEnd), leftMargin + 250f, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.1f kWh", report.normalConsumption), leftMargin + 350f, y, paint)
        y += 20f

        // Wärmepumpe row
        canvas.drawText("Wärmepumpe", leftMargin, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.1f kWh", report.heatPumpStart), leftMargin + 150f, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.1f kWh", report.heatPumpEnd), leftMargin + 250f, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.1f kWh", report.heatPumpConsumption), leftMargin + 350f, y, paint)
        y += 22f

        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 18f

        // Gesamtverbrauch right-aligned under readings table
        val consumptionPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 12f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Gesamtverbrauch: ${String.format(Locale.GERMAN, "%.1f kWh", report.totalConsumption)}", rightMargin, y, consumptionPaint)
        y += 25f

        // Costs Breakdown
        canvas.drawText("Kostenberechnung & Tarife", leftMargin, y, headerPaint)
        y += 18f

        val rightPaint = Paint(paint).apply { textAlign = Paint.Align.RIGHT }
        val rightHeaderPaint = Paint(headerPaint).apply { textAlign = Paint.Align.RIGHT }
        val costLabelX = leftMargin + 10f
        val costTariffX = leftMargin + 180f

        canvas.drawText("1. Tag-Strom-Tarif:", leftMargin, y, subtitlePaint)
        y += 15f
        canvas.drawText("• Arbeitspreis:", costLabelX, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.2f ct/kWh", tariffs.normalWorkPriceCt), costTariffX, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.2f €", report.normalWorkCost), rightMargin, y, rightPaint)
        y += 15f
        canvas.drawText("• Grundgebühr:", costLabelX, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.2f €/Monat", tariffs.normalBaseFeeEurMonth), costTariffX, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.2f €", report.normalBaseCost), rightMargin, y, rightPaint)
        y += 16f
        canvas.drawText("Zwischensumme Tag-Strom:", costLabelX, y, headerPaint)
        canvas.drawText(String.format(Locale.GERMAN, "%.2f €", report.normalTotalCost), rightMargin, y, rightHeaderPaint)
        y += 24f

        canvas.drawText("2. Wärmepumpen-Tarif:", leftMargin, y, subtitlePaint)
        y += 15f
        canvas.drawText("• Arbeitspreis:", costLabelX, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.2f ct/kWh", tariffs.heatPumpWorkPriceCt), costTariffX, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.2f €", report.heatPumpWorkCost), rightMargin, y, rightPaint)
        y += 15f
        canvas.drawText("• Grundgebühr:", costLabelX, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.2f €/Monat", tariffs.heatPumpBaseFeeEurMonth), costTariffX, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.2f €", report.heatPumpBaseCost), rightMargin, y, rightPaint)
        y += 16f
        canvas.drawText("Zwischensumme Wärmepumpe:", costLabelX, y, headerPaint)
        canvas.drawText(String.format(Locale.GERMAN, "%.2f €", report.heatPumpTotalCost), rightMargin, y, rightHeaderPaint)
        y += 25f

        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 22f

        val totalPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 14f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("Gesamtkosten: ${String.format(Locale.GERMAN, "%.2f €", report.totalCost)}", rightMargin, y, totalPaint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Stromzaehler_Bericht_${report.endString}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }
}
