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
        val leftMargin = 80f
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

        val hasRegularNote = report.note.trim().isNotBlank()
        val hasHeatingNotes = report.heatingNotes.isNotEmpty()
        if (hasRegularNote || hasHeatingNotes) {
            y += 22f // Eine Leerzeile zwischen Zeitraum und Notizen
            val noteBoldPaint = Paint(paint).apply {
                color = Color.parseColor("#0F172A")
                isFakeBoldText = true
            }
            val notePaint = Paint(paint).apply {
                color = Color.parseColor("#334155")
            }

            if (hasRegularNote) {
                val prefix = "Notiz: "
                val prefixWidth = noteBoldPaint.measureText(prefix)
                val availableWidth = (rightMargin - leftMargin) - prefixWidth
                val wrappedLines = wrapText(report.note.trim(), notePaint, availableWidth)

                for ((index, line) in wrappedLines.withIndex()) {
                    if (y > 430f) {
                        canvas.drawText("...", leftMargin + prefixWidth, y, notePaint)
                        y += 14f
                        break
                    }
                    if (index == 0) {
                        canvas.drawText(prefix, leftMargin, y, noteBoldPaint)
                    }
                    if (line.isNotEmpty()) {
                        canvas.drawText(line, leftMargin + prefixWidth, y, notePaint)
                    }
                    y += 14f
                }
            }

            if (hasHeatingNotes) {
                val headerPaintBold = Paint(headerPaint).apply { textSize = 9.5f; color = Color.parseColor("#0F172A") }
                val subBoldPaint = Paint(headerPaint).apply { textSize = 9f; color = Color.parseColor("#1E293B") }
                val indent = 14f
                val availableSubWidth = (rightMargin - leftMargin) - indent

                for (hNote in report.heatingNotes) {
                    val lines = hNote.split("\n")
                    for ((lIdx, lineStr) in lines.withIndex()) {
                        if (y > 450f) {
                            canvas.drawText("...", leftMargin + indent, y, notePaint)
                            y += 13f
                            break
                        }
                        if (lIdx == 0) {
                            val cleanHeader = if (lineStr.startsWith("Heizung (") && lineStr.endsWith("):")) {
                                "Heizung (Extra-Notiz · " + lineStr.removePrefix("Heizung (")
                            } else {
                                lineStr
                            }
                            canvas.drawText(cleanHeader, leftMargin, y, headerPaintBold)
                            y += 13f
                        } else {
                            val trimmed = lineStr.trim()
                            val isEg = trimmed.startsWith("• EG:") || trimmed.startsWith("EG:")
                            val isOg = trimmed.startsWith("• OG:") || trimmed.startsWith("OG:")
                            val prefix = if (isEg) "• EG: " else if (isOg) "• OG: " else "• "
                            val content = trimmed.removePrefix("• ").removePrefix("EG: ").removePrefix("OG: ").trim()

                            val pfxWidth = subBoldPaint.measureText(prefix)
                            val wrapped = wrapText(content, notePaint, availableSubWidth - pfxWidth)
                            for ((wIdx, wLine) in wrapped.withIndex()) {
                                if (y > 450f) break
                                if (wIdx == 0) {
                                    canvas.drawText(prefix, leftMargin + indent, y, subBoldPaint)
                                    canvas.drawText(wLine, leftMargin + indent + pfxWidth, y, notePaint)
                                } else {
                                    canvas.drawText(wLine, leftMargin + indent + pfxWidth, y, notePaint)
                                }
                                y += 13f
                            }
                        }
                    }
                }
            }
            y += 10f
        } else {
            y += 24f
        }

        // Meter Readings Table Header
        canvas.drawText("Zählerstände & Verbrauch", leftMargin, y, headerPaint)
        y += 16f

        canvas.drawText("Zähler", leftMargin, y, subtitlePaint)
        canvas.drawText("Alt", leftMargin + 135f, y, subtitlePaint)
        canvas.drawText("Neu", leftMargin + 240f, y, subtitlePaint)
        canvas.drawText("Verbrauch", leftMargin + 345f, y, subtitlePaint)
        y += 6f
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 18f

        // Tag-Strom row
        canvas.drawText("Tag-Strom", leftMargin, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.1f kWh", report.normalStart), leftMargin + 135f, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.1f kWh", report.normalEnd), leftMargin + 240f, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.1f kWh", report.normalConsumption), leftMargin + 345f, y, paint)
        y += 20f

        // Wärmepumpe row
        canvas.drawText("Wärmepumpe", leftMargin, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.1f kWh", report.heatPumpStart), leftMargin + 135f, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.1f kWh", report.heatPumpEnd), leftMargin + 240f, y, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.1f kWh", report.heatPumpConsumption), leftMargin + 345f, y, paint)
        y += 22f

        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 18f

        // Gesamtverbrauch right-aligned under readings table
        val consumptionPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 11.5f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        val dailyAvg = report.dailyAverageConsumption
        canvas.drawText("Gesamtverbrauch: ${String.format(Locale.GERMAN, "%.1f kWh", report.totalConsumption)}  (Ø ${String.format(Locale.GERMAN, "%.2f kWh/Tag", dailyAvg)} bei ${report.days} Tagen)", rightMargin, y, consumptionPaint)
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

        // Footer
        val footerY = 800f
        val footerPaint = Paint().apply { color = Color.BLACK; textSize = 8f }
        canvas.drawLine(leftMargin, footerY, rightMargin, footerY, linePaint)
        canvas.drawText("Stromzähler-App", leftMargin, footerY + 15f, footerPaint)
        val pageNumPaint = Paint(footerPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("Seite 1 von 1", rightMargin, footerY + 15f, pageNumPaint)

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

    fun generateAnalyticsReportPdf(
        context: Context,
        intervals: List<IntervalReport>,
        monthKey: String,
        tariffs: TariffSettingsEntity,
        property: PropertySettingsEntity?,
        tenant: TenantEntity?
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { color = Color.BLACK; textSize = 9f }
        val smallPaint = Paint().apply { color = Color.BLACK; textSize = 8f }
        val titlePaint = Paint().apply { color = Color.parseColor("#1E293B"); textSize = 16f; isFakeBoldText = true }
        val headerPaint = Paint().apply { color = Color.parseColor("#0F172A"); textSize = 10f; isFakeBoldText = true }
        val linePaint = Paint().apply { color = Color.parseColor("#CBD5E1"); strokeWidth = 1f }
        val boxPaint = Paint().apply { color = Color.parseColor("#F1F5F9") }

        var y = 45f
        val leftMargin = 80f 
        val rightMargin = 555f
        val chartLeftPadding = 50f
        val chartRightPadding = 20f

        // Title
        val yearPart = monthKey.split("-")[0]
        val monthNum = monthKey.split("-")[1].toInt()
        val monthNames = arrayOf("", "Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember")
        val monthName = monthNames.getOrElse(monthNum) { "" }

        canvas.drawText("Grafische Auswertung - $monthName - $yearPart", leftMargin, y, titlePaint)
        y += 25f
        canvas.drawText("Erstellt am: ${SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN).format(Date())}", leftMargin, y, paint)
        y += 30f
        if (property != null) {
            canvas.drawText("Mietobjekt: ${property.propertyName}", leftMargin, y, headerPaint)
            y += 15f
            if (property.propertyAddress.isNotBlank()) {
                canvas.drawText("Anschrift: ${property.propertyAddress}", leftMargin, y, paint)
                y += 20f
            }
        }
        y += 5f

        // Summary Tiles
        val totalCons = intervals.sumOf { it.totalConsumption }
        val totalNormal = intervals.sumOf { it.normalConsumption }
        val totalHp = intervals.sumOf { it.heatPumpConsumption }
        val totalCost = intervals.sumOf { it.totalCost }
        val totalDays = intervals.sumOf { it.days }.let { if (it == 0) 1 else it }

        val valAlignRightPaint = Paint(paint).apply { textAlign = Paint.Align.RIGHT }
        val headerAlignRightPaint = Paint(headerPaint).apply { textAlign = Paint.Align.RIGHT }
        val valRightX = leftMargin + 230f

        canvas.drawRect(leftMargin, y, rightMargin, y + 80f, boxPaint)
        canvas.drawText("Gesamt:", leftMargin + 10f, y + 20f, headerPaint)
        canvas.drawText(String.format(Locale.GERMAN, "%.1f kWh", totalCons), valRightX, y + 20f, headerAlignRightPaint)
        canvas.drawText("Tag-Strom:", leftMargin + 10f, y + 35f, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.1f kWh", totalNormal), valRightX, y + 35f, valAlignRightPaint)
        canvas.drawText("Wärmepumpe:", leftMargin + 10f, y + 50f, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.1f kWh", totalHp), valRightX, y + 50f, valAlignRightPaint)
        canvas.drawText("Ø/Tag:", leftMargin + 10f, y + 65f, paint)
        canvas.drawText(String.format(Locale.GERMAN, "%.2f kWh", totalCons / totalDays), valRightX, y + 65f, valAlignRightPaint)
        canvas.drawText("Gesamtkosten:", leftMargin + 300f, y + 20f, headerPaint)
        canvas.drawText(String.format(Locale.GERMAN, "%.2f €", totalCost), leftMargin + 300f, y + 45f, titlePaint)
        
        y += 100f

        // Chart Rendering
        canvas.drawText("Verbrauch je Intervall (kWh)", leftMargin, y, headerPaint)
        y += 20f
        
        val chartY = y
        val chartHeight = 150f
        val chartWidth = rightMargin - leftMargin - chartLeftPadding - chartRightPadding
        val maxY = intervals.maxOfOrNull { maxOf(it.normalConsumption, it.heatPumpConsumption, it.dailyAverageConsumption) }?.let { if (it == 0.0) 10.0 else it * 1.1 } ?: 10.0
        
        // Raster lines
        for (i in 0..4) {
            val yVal = chartY + chartHeight - (i * chartHeight / 4f)
            canvas.drawLine(leftMargin + chartLeftPadding, yVal, rightMargin, yVal, linePaint)
        }
        
        // Axis
        canvas.drawLine(leftMargin + chartLeftPadding, chartY, leftMargin + chartLeftPadding, chartY + chartHeight, linePaint)
        for (i in 0..4) {
            val yVal = chartY + chartHeight - (i * chartHeight / 4f)
            val kwhVal = (maxY * i / 4f)
            canvas.drawText(String.format(Locale.GERMAN, "%.0f", kwhVal), leftMargin + 5f, yVal + 3f, paint)
        }
        
        // Bars/Circles
        val barCount = intervals.size
        val barGroupWidth = chartWidth / barCount
        val circleRadius = 2.5f
        val barWidth = barGroupWidth * 0.2f
        
        intervals.sortedBy { it.startDate }.forEachIndexed { index, report ->
            val x = (leftMargin + chartLeftPadding + (index * barGroupWidth) + (barGroupWidth * 0.1f)).toFloat()
            val avgColor = Color.parseColor("#8B5CF6")
            
            // Average (Circle)
            val avgH = (report.dailyAverageConsumption / maxY) * chartHeight
            val circleY = (chartY + chartHeight - avgH).toFloat()
            canvas.drawCircle(x + 5f, circleY, circleRadius, Paint().apply { color = avgColor })
            canvas.drawLine(x + 5f, circleY, x + 5f, chartY + chartHeight, Paint().apply { color = avgColor; strokeWidth = 0.5f })
            if (report.dailyAverageConsumption > 0) {
                canvas.drawText(
                    String.format(Locale.GERMAN, "%.1f", report.dailyAverageConsumption),
                    x - 2f,
                    circleY - 5f,
                    Paint(smallPaint).apply { textAlign = Paint.Align.RIGHT }
                )
            }
            
            // Normal (Bar)
            val normalH = (report.normalConsumption / maxY) * chartHeight
            canvas.drawRect(x + 10f, chartY + chartHeight - normalH.toFloat(), x + 10f + barWidth.toFloat(), chartY + chartHeight, Paint().apply { color = Color.parseColor("#3B82F6") })
            if (report.normalConsumption > 0) {
                canvas.drawText(String.format(Locale.GERMAN, "%.1f", report.normalConsumption), x + 10f + (barWidth.toFloat() / 2), chartY + chartHeight - normalH.toFloat() - 8f, smallPaint)
            }
            
            // Heat Pump (Bar)
            val hpH = (report.heatPumpConsumption / maxY) * chartHeight
            canvas.drawRect(x + 10f + barWidth.toFloat() + 2f, chartY + chartHeight - hpH.toFloat(), x + 10f + (barWidth.toFloat() * 2) + 2f, chartY + chartHeight, Paint().apply { color = Color.parseColor("#10B981") })
            if (report.heatPumpConsumption > 0) {
                canvas.drawText(String.format(Locale.GERMAN, "%.1f", report.heatPumpConsumption), x + 10f + barWidth.toFloat() + 2f + (barWidth.toFloat() / 2), chartY + chartHeight - hpH.toFloat() - 8f, smallPaint)
            }
            
            // Date label
            canvas.drawText("${com.example.utils.DateUtils.formatToGermanDate(report.startString).take(5)} - ${com.example.utils.DateUtils.formatToGermanDate(report.endString).take(5)}", x, chartY + chartHeight + 15f, smallPaint)
        }
        y += chartHeight + 15f
        
        // Legend
        val legendY = y + 35f
        canvas.drawCircle(leftMargin + 10f, legendY, 3f, Paint().apply { color = Color.parseColor("#8B5CF6") })
        canvas.drawText("Durchschnitt pro Tag", leftMargin + 20f, legendY + 3f, smallPaint)
        
        canvas.drawRect(leftMargin + 130f, legendY - 3f, leftMargin + 140f, legendY + 3f, Paint().apply { color = Color.parseColor("#3B82F6") })
        canvas.drawText("Tag-Strom", leftMargin + 145f, legendY + 3f, smallPaint)
        
        canvas.drawRect(leftMargin + 220f, legendY - 3f, leftMargin + 230f, legendY + 3f, Paint().apply { color = Color.parseColor("#10B981") })
        canvas.drawText("Wärmepumpe", leftMargin + 235f, legendY + 3f, smallPaint)
        
        y = legendY + 28f
        
        // Table
        canvas.drawText("Detailaufstellung:", leftMargin, y, headerPaint)
        y += 15f
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 15f
        canvas.drawText("Zeitraum", leftMargin, y, headerPaint)
        canvas.drawText("Verbrauch", leftMargin + 180f, y, headerPaint)
        canvas.drawText("Ø/Tag", leftMargin + 280f, y, headerPaint)
        canvas.drawText("Kosten", leftMargin + 380f, y, headerPaint)
        y += 5f
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 18f

        val zebraPaint = Paint().apply { color = Color.parseColor("#F1F5F9") }
        intervals.sortedBy { it.startDate }.forEachIndexed { index, report ->
            if (y > 720f) return@forEachIndexed
            if (index % 2 == 0) {
                canvas.drawRect(leftMargin, y - 12f, rightMargin, y + 6f, zebraPaint)
            }
            canvas.drawText("${com.example.utils.DateUtils.formatToGermanDate(report.startString)} - ${com.example.utils.DateUtils.formatToGermanDate(report.endString)}", leftMargin, y, paint)
            canvas.drawText(String.format(Locale.GERMAN, "%.1f kWh", report.totalConsumption), leftMargin + 180f, y, paint)
            canvas.drawText(String.format(Locale.GERMAN, "%.2f", report.dailyAverageConsumption), leftMargin + 280f, y, paint)
            canvas.drawText(String.format(Locale.GERMAN, "%.2f €", report.totalCost), leftMargin + 380f, y, smallPaint)
            y += 16f
        }

        // Chronological Notes under Detailaufstellung (only if notes or heating extra-notes exist)
        val notesList = intervals
            .filter { it.note.trim().isNotBlank() || it.heatingNotes.isNotEmpty() }
            .sortedBy { it.startDate }

        if (notesList.isNotEmpty()) {
            y += 14f
            canvas.drawText("Notizen:", leftMargin, y, headerPaint)
            y += 15f
            val notePaint = Paint(paint).apply { color = Color.parseColor("#334155") }
            val noteBoldPaint = Paint(headerPaint).apply { textSize = 9f; color = Color.parseColor("#1E293B") }
            val maxTextWidth = rightMargin - leftMargin

            notesList.forEach { report ->
                if (y > 750f) return@forEach
                val repStart = com.example.utils.DateUtils.formatToGermanDate(report.startString)
                val repEnd = com.example.utils.DateUtils.formatToGermanDate(report.endString)
                val dateRangeStr = if (repStart != repEnd) " (${repStart.take(5)} - ${repEnd.take(5)})" else ""
                val prefix = "• $repEnd$dateRangeStr: "
                val prefixWidth = noteBoldPaint.measureText(prefix)

                val combinedParts = mutableListOf<String>()
                if (report.note.trim().isNotBlank()) {
                    combinedParts.add(report.note.trim())
                }
                report.heatingNotes.forEach { hNote ->
                    combinedParts.add("Extra-Notiz · $hNote")
                }
                val noteText = combinedParts.joinToString("\n")
                val paragraphs = noteText.split("\n")

                paragraphs.forEachIndexed { pIdx, paragraph ->
                    if (y > 755f) return@forEach
                    val words = paragraph.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                    if (words.isEmpty()) return@forEachIndexed

                    if (pIdx == 0) {
                        var curLine = ""
                        var prefixDrawn = false

                        for (word in words) {
                            val testRemainder = if (curLine.isEmpty()) word else "$curLine $word"
                            val neededWidth = if (!prefixDrawn) prefixWidth + notePaint.measureText(testRemainder) else 12f + notePaint.measureText(testRemainder)

                            if (neededWidth <= maxTextWidth) {
                                curLine = testRemainder
                            } else {
                                if (!prefixDrawn) {
                                    canvas.drawText(prefix, leftMargin, y, noteBoldPaint)
                                    canvas.drawText(curLine, leftMargin + prefixWidth, y, notePaint)
                                    prefixDrawn = true
                                } else {
                                    canvas.drawText(curLine, leftMargin + 12f, y, notePaint)
                                }
                                y += 13f
                                if (y > 755f) return@forEach
                                curLine = word
                            }
                        }

                        if (!prefixDrawn) {
                            canvas.drawText(prefix, leftMargin, y, noteBoldPaint)
                            canvas.drawText(curLine, leftMargin + prefixWidth, y, notePaint)
                        } else if (curLine.isNotEmpty()) {
                            canvas.drawText(curLine, leftMargin + 12f, y, notePaint)
                        }
                        y += 14f
                    } else {
                        val trimmed = paragraph.trim()
                        val isEg = trimmed.startsWith("• EG:") || trimmed.startsWith("EG:")
                        val isOg = trimmed.startsWith("• OG:") || trimmed.startsWith("OG:")
                        val subPrefix = if (isEg) "• EG: " else if (isOg) "• OG: " else ""
                        val content = if (subPrefix.isNotEmpty()) {
                            trimmed.removePrefix("• ").removePrefix("EG:").removePrefix("OG:").trim()
                        } else {
                            trimmed
                        }
                        val indent = if (subPrefix.isNotEmpty()) 14f else 12f
                        val subPfxWidth = if (subPrefix.isNotEmpty()) noteBoldPaint.measureText(subPrefix) else 0f
                        val availableWidth = maxTextWidth - indent - subPfxWidth
                        val wrapped = wrapText(content, notePaint, availableWidth)

                        for ((wIdx, wLine) in wrapped.withIndex()) {
                            if (y > 755f) return@forEach
                            if (wIdx == 0) {
                                if (subPrefix.isNotEmpty()) {
                                    canvas.drawText(subPrefix, leftMargin + indent, y, noteBoldPaint)
                                    canvas.drawText(wLine, leftMargin + indent + subPfxWidth, y, notePaint)
                                } else {
                                    canvas.drawText(wLine, leftMargin + indent, y, notePaint)
                                }
                            } else {
                                val offset = if (subPrefix.isNotEmpty()) leftMargin + indent + subPfxWidth else leftMargin + indent
                                canvas.drawText(wLine, offset, y, notePaint)
                            }
                            y += 13f
                        }
                    }
                }
                y += 2f
            }
        }

        // Tariff Info
        val tariffY = maxOf(y + 10f, 762f).coerceAtMost(775f)
        canvas.drawText("Tarife:", leftMargin, tariffY, headerPaint)
        canvas.drawText("Tag-Strom: ${String.format(Locale.GERMAN, "%.2f ct/kWh", tariffs.normalWorkPriceCt)} | Wärmepumpe: ${String.format(Locale.GERMAN, "%.2f ct/kWh", tariffs.heatPumpWorkPriceCt)}", leftMargin, tariffY + 14f, smallPaint)

        // Footer
        val footerY = 800f
        canvas.drawLine(leftMargin, footerY, rightMargin, footerY, linePaint)
        canvas.drawText("Stromzähler-App", leftMargin, footerY + 15f, smallPaint)
        val pageNumPaint = Paint(smallPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("Seite 1 von 1", rightMargin, footerY + 15f, pageNumPaint)

        pdfDocument.finishPage(page)
        val file = File(context.cacheDir, "Auswertung_${monthKey}.pdf")
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

    fun generateHeatingGuidePdf(
        context: Context,
        property: PropertySettingsEntity?,
        latestSettings: com.example.data.HeatingSettingsEntity? = null
    ): File? {
        val pdfDocument = PdfDocument()
        val totalPages = if (latestSettings != null) 2 else 1
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val leftMargin = 80f
        val rightMargin = 555f
        val maxWidth = rightMargin - leftMargin

        val titlePaint = Paint().apply { color = Color.parseColor("#1E293B"); textSize = 16f; isFakeBoldText = true }
        val sectionPaint = Paint().apply { color = Color.parseColor("#0F172A"); textSize = 11.5f; isFakeBoldText = true }
        val boldPaint = Paint().apply { color = Color.parseColor("#1E293B"); textSize = 9f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { color = Color.parseColor("#334155"); textSize = 9f }
        val smallPaint = Paint().apply { color = Color.BLACK; textSize = 8f }
        val linePaint = Paint().apply { color = Color.parseColor("#CBD5E1"); strokeWidth = 1f }
        val boxPaint = Paint().apply { color = Color.parseColor("#F1F5F9") }
        val warnBoxPaint = Paint().apply { color = Color.parseColor("#FEF2F2") }
        val warnTextPaint = Paint().apply { color = Color.parseColor("#991B1B"); textSize = 8.5f; isFakeBoldText = true }

        var y = 42f

        canvas.drawText("Heizungs-Kurzanleitung (EG & OG)", leftMargin, y, titlePaint)
        y += 16f
        val genDate = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN).format(Date())
        val propSuffix = if (property != null && property.propertyName.isNotBlank()) " | ${property.propertyName}" else ""
        canvas.drawText("Stand: $genDate$propSuffix", leftMargin, y, smallPaint)
        y += 14f
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 18f

        // --- SECTION 1: EG ACALOR ---
        canvas.drawText("1. Erdgeschoss (EG): ACALOR Klimaregler KTRRUU-212.170", leftMargin, y, sectionPaint)
        y += 15f

        val egSteps = listOf(
            "1. Menü aufrufen: Menü-Taste (M / drei Linien) drücken -> 'Tagesprogramm Heizen' wählen -> OK.",
            "2. Tag wählen: Gewünschten Wochentag (z. B. Mo) oder 'All' für alle Tage wählen -> OK.",
            "3. Zeitleiste: Start bei 00:00 Uhr, Stunde für Stunde Modus festlegen, nach 24:00 Uhr mit OK speichern."
        )
        for (step in egSteps) {
            for (line in wrapText(step, bodyPaint, maxWidth)) {
                canvas.drawText(line, leftMargin, y, bodyPaint)
                y += 12f
            }
        }
        y += 4f

        canvas.drawRect(leftMargin, y - 10f, rightMargin, y + 6f, boxPaint)
        canvas.drawText("Zeitraum", leftMargin + 6f, y + 1f, boldPaint)
        canvas.drawText("Modus", leftMargin + 110f, y + 1f, boldPaint)
        canvas.drawText("Soll-Temp.", leftMargin + 215f, y + 1f, boldPaint)
        canvas.drawText("Display-Anzeige", leftMargin + 295f, y + 1f, boldPaint)
        y += 18f

        for (slot in com.example.data.HeatingGuideData.acalorSchedule) {
            canvas.drawText(slot.timeLabel, leftMargin + 6f, y, bodyPaint)
            canvas.drawText(slot.modeName, leftMargin + 110f, y, bodyPaint)
            canvas.drawText(slot.temperature, leftMargin + 215f, y, bodyPaint)
            canvas.drawText(slot.displaySymbol, leftMargin + 295f, y, bodyPaint)
            y += 13f
        }
        y += 4f

        val coolingNote = "Kühlbetrieb im Sommer: Menü-Taste -> 'Betriebsart'/'Modus' von 'Heizen' auf 'Kühlen' (Schneeflocke) + OK. Separate Temperaturen (z. B. 24 °C) unter 'Tagesprogramm Kühlen'."
        for (line in wrapText(coolingNote, bodyPaint, maxWidth)) {
            canvas.drawText(line, leftMargin, y, bodyPaint)
            y += 12f
        }
        y += 10f
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 18f

        // --- SECTION 2: OG THERMOSTAT ---
        canvas.drawText("2. Obergeschoss (OG): Raumthermostat Programme", leftMargin, y, sectionPaint)
        y += 14f
        canvas.drawText("Einstellungen: Heizen = 18,5 °C | Abgesenkt = 14 °C | Frostschutz = 5 °C (nur P0)", leftMargin, y, boldPaint)
        y += 13f

        val sunTip = "Kurzfristig aufheizen (Sonne-Taste): In der abgesenkten Zeit einfach Taste mit der Sonne drücken. Heizt bis zur nächsten Programmschaltung, danach läuft das normale Programm weiter."
        for (line in wrapText(sunTip, bodyPaint, maxWidth)) {
            canvas.drawText(line, leftMargin, y, bodyPaint)
            y += 12f
        }
        y += 5f

        for (prog in com.example.data.HeatingGuideData.ogPrograms) {
            canvas.drawText("P${prog.number}:", leftMargin + 4f, y, boldPaint)
            canvas.drawText(prog.summary, leftMargin + 36f, y, bodyPaint)
            y += 12.5f
        }
        y += 6f

        canvas.drawRect(leftMargin, y - 10f, rightMargin, y + 20f, warnBoxPaint)
        val warningText = "Achtung Programme 12–20: Nicht eingestellt! Nicht benutzen oder erst individuell einstellen, sonst läuft die Heizung 24 Std. durch wie bei Programm 8."
        for (line in wrapText(warningText, warnTextPaint, maxWidth - 12f)) {
            canvas.drawText(line, leftMargin + 6f, y + 1f, warnTextPaint)
            y += 12f
        }

        // Footer Page 1
        val footerY = 800f
        canvas.drawLine(leftMargin, footerY, rightMargin, footerY, linePaint)
        canvas.drawText("Stromzähler-App", leftMargin, footerY + 15f, smallPaint)
        val pageNumPaint = Paint(smallPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("Seite 1 von $totalPages", rightMargin, footerY + 15f, pageNumPaint)

        pdfDocument.finishPage(page)

        // Optional Page 2: Currently saved settings
        if (latestSettings != null) {
            val page2Info = PdfDocument.PageInfo.Builder(595, 842, 2).create()
            val page2 = pdfDocument.startPage(page2Info)
            val c2 = page2.canvas
            var y2 = 45f

            c2.drawText("Aktuell gespeicherte Heizungseinstellungen", leftMargin, y2, titlePaint)
            y2 += 16f
            val effDate = com.example.utils.DateUtils.formatToGermanDate(latestSettings.effectiveDateString)
            c2.drawText("Gültig ab: $effDate$propSuffix", leftMargin, y2, smallPaint)
            y2 += 14f
            c2.drawLine(leftMargin, y2, rightMargin, y2, linePaint)
            y2 += 20f

            // EG Summary
            c2.drawText("Erdgeschoss (EG) · ACALOR KTRRUU-212.170", leftMargin, y2, sectionPaint)
            y2 += 16f
            val egModeStr = if (latestSettings.egMode == "KUEHLEN") {
                "Betriebsart: Kühlen | Komfortkühlung: ${String.format(Locale.GERMAN, "%.1f °C", latestSettings.egCoolTemp)}"
            } else {
                "Betriebsart: Heizen | Komfort: ${String.format(Locale.GERMAN, "%.1f °C", latestSettings.egComfortTemp)} | ECO: ${String.format(Locale.GERMAN, "%.1f °C", latestSettings.egEcoTemp)}"
            }
            c2.drawText(egModeStr, leftMargin, y2, bodyPaint)
            y2 += 16f

            if (latestSettings.egScheduleType == "WEEKLY") {
                c2.drawText("Wochenplan (Mo–So):", leftMargin, y2, boldPaint)
                y2 += 14f
                val weekly = com.example.data.HeatingGuideData.parseWeeklyMasks(latestSettings.egWeeklyMasks)
                com.example.data.HeatingGuideData.WEEKDAYS.forEachIndexed { idx, day ->
                    c2.drawText("$day:", leftMargin + 6f, y2, boldPaint)
                    c2.drawText(com.example.data.HeatingGuideData.formatHourRanges(weekly[idx]), leftMargin + 36f, y2, bodyPaint)
                    y2 += 14f
                }
            } else {
                val hours = com.example.data.HeatingGuideData.parseEgMask(latestSettings.egAllDaysMask)
                c2.drawText("Alle Tage (All): ${com.example.data.HeatingGuideData.formatHourRanges(hours)}", leftMargin, y2, bodyPaint)
                y2 += 16f
            }

            y2 += 12f
            c2.drawLine(leftMargin, y2, rightMargin, y2, linePaint)
            y2 += 20f

            // OG Summary
            c2.drawText("Obergeschoss (OG) · Raumthermostat", leftMargin, y2, sectionPaint)
            y2 += 16f
            c2.drawText(
                "Heizen: ${String.format(Locale.GERMAN, "%.1f °C", latestSettings.ogHeatTemp)} | Abgesenkt: ${String.format(Locale.GERMAN, "%.1f °C", latestSettings.ogSetbackTemp)} | Frostschutz: ${String.format(Locale.GERMAN, "%.1f °C", latestSettings.ogFrostTemp)}",
                leftMargin,
                y2,
                bodyPaint
            )
            y2 += 18f

            val ogProgs = com.example.data.HeatingGuideData.parseOgDayPrograms(latestSettings.ogDayPrograms)
            com.example.data.HeatingGuideData.WEEKDAYS.forEachIndexed { idx, day ->
                val pNum = ogProgs[idx]
                val pSummary = com.example.data.HeatingGuideData.ogPrograms.find { it.number == pNum }?.summary ?: ""
                c2.drawText("$day:", leftMargin + 6f, y2, boldPaint)
                c2.drawText("P$pNum – $pSummary", leftMargin + 36f, y2, bodyPaint)
                y2 += 14f
            }

            // Footer Page 2
            c2.drawLine(leftMargin, footerY, rightMargin, footerY, linePaint)
            c2.drawText("Stromzähler-App", leftMargin, footerY + 15f, smallPaint)
            c2.drawText("Seite 2 von $totalPages", rightMargin, footerY + 15f, pageNumPaint)

            pdfDocument.finishPage(page2)
        }

        val file = File(context.cacheDir, "Heizungs_Kurzanleitung_EG_OG.pdf")
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

    internal fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        val paragraphs = text.split("\n")
        for (paragraph in paragraphs) {
            val trimmed = paragraph.trim()
            if (trimmed.isEmpty()) {
                if (result.isNotEmpty()) result.add("")
                continue
            }
            val words = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
            var currentLine = StringBuilder()
            for (word in words) {
                // If a single word is wider than maxWidth, split it into chunks
                if (paint.measureText(word) > maxWidth) {
                    if (currentLine.isNotEmpty()) {
                        result.add(currentLine.toString())
                        currentLine = StringBuilder()
                    }
                    var chunk = StringBuilder()
                    for (ch in word) {
                        if (paint.measureText(chunk.toString() + ch) <= maxWidth) {
                            chunk.append(ch)
                        } else {
                            if (chunk.isNotEmpty()) {
                                result.add(chunk.toString())
                                chunk = StringBuilder()
                            }
                            chunk.append(ch)
                        }
                    }
                    if (chunk.isNotEmpty()) {
                        currentLine = chunk
                    }
                    continue
                }

                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine = StringBuilder(testLine)
                } else {
                    if (currentLine.isNotEmpty()) {
                        result.add(currentLine.toString())
                    }
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty()) {
                result.add(currentLine.toString())
            }
        }
        return result
    }
}
