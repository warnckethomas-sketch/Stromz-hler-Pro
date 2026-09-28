package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IntervalReport
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private fun calculateNiceMax(value: Double): Double {
    if (value <= 4.0) return 4.0
    val magnitude = 10.0.pow(floor(log10(value)))
    val normalized = value / magnitude
    val niceNormalized = when {
        normalized <= 1.0 -> 1.0
        normalized <= 2.0 -> 2.0
        normalized <= 4.0 -> 4.0
        normalized <= 6.0 -> 6.0
        normalized <= 8.0 -> 8.0
        else -> 10.0
    }
    return niceNormalized * magnitude
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    intervals: List<IntervalReport>,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grafische Auswertung", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingVals ->
        if (intervals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingVals)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Keine Intervalldaten vorhanden",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Erfassen Sie mindestens zwei Zählerstände, um Verbrauchsintervalle auszuwerten.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Group intervals by month (ascending order: e.g. 2026-01, 2026-02, 2026-03)
            val intervalsByMonth = remember(intervals) {
                intervals.groupBy { extractMonthKey(it.endString) }
                    .toSortedMap()
            }
            val monthKeys = remember(intervalsByMonth) { intervalsByMonth.keys.toList() }

            // Default to current month if in list, otherwise the newest/last month
            val currentMonthKey = remember {
                SimpleDateFormat("yyyy-MM", Locale.GERMAN).format(java.util.Date())
            }
            val initialIndex = remember(monthKeys) {
                val idx = monthKeys.indexOf(currentMonthKey)
                if (idx != -1) idx else (monthKeys.size - 1).coerceAtLeast(0)
            }

            val pagerState = rememberPagerState(initialPage = initialIndex) { monthKeys.size }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingVals)
            ) {
                // ==========================================
                // MONATS-KOPFZEILE MIT PFEILEN & WISCH-HINWEIS
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (pagerState.currentPage > 0) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage > 0
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Vorheriger Monat",
                                tint = if (pagerState.currentPage > 0) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f)
                                }
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val activeYearMonth = monthKeys.getOrNull(pagerState.currentPage) ?: ""
                            Text(
                                formatMonthYear(activeYearMonth),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Monat ${pagerState.currentPage + 1} von ${monthKeys.size} • Wischen nach links / rechts",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (pagerState.currentPage < monthKeys.size - 1) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage < monthKeys.size - 1
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Nächster Monat",
                                tint = if (pagerState.currentPage < monthKeys.size - 1) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f)
                                }
                            )
                        }
                    }
                }

                // ==========================================
                // HORIZONTAL PAGER: MONATLICHE AUSWERTUNG
                // ==========================================
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val yearMonth = monthKeys.getOrNull(pageIndex) ?: return@HorizontalPager
                    val monthIntervals = intervalsByMonth[yearMonth] ?: emptyList()
                    val monthTitle = formatMonthYear(yearMonth)

                    // Chronological order for the chart (left = earliest in month, right = latest in month)
                    val chartIntervals = remember(monthIntervals) {
                        monthIntervals.sortedBy { it.startDate }
                    }

                    val totalMonthCons = monthIntervals.sumOf { it.totalConsumption }
                    val totalMonthCost = monthIntervals.sumOf { it.totalCost }
                    val normalMonthCons = monthIntervals.sumOf { it.normalConsumption }
                    val hpMonthCons = monthIntervals.sumOf { it.heatPumpConsumption }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Monats-KPI Zusammenfassung
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Gesamt im $monthTitle",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            String.format(Locale.GERMAN, "%.2f €", totalMonthCost),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 1. Kachel: Tag-Strom
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .background(
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                                                shape = RoundedCornerShape(8.dp)
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Bolt,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        "Tag-Strom",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Text(
                                                    String.format(Locale.GERMAN, "%.1f kWh", normalMonthCons),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        // 2. Kachel: Wärmepumpe
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .background(
                                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                                                                shape = RoundedCornerShape(8.dp)
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Default.ElectricMeter,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.secondary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        "Wärmepumpe",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Text(
                                                    String.format(Locale.GERMAN, "%.1f kWh", hpMonthCons),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }

                                        // 3. Kachel: Gesamt-Verbrauch
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .background(
                                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                                                shape = RoundedCornerShape(8.dp)
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Default.ElectricMeter,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        "Gesamt-Verbrauch",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Text(
                                                    String.format(Locale.GERMAN, "%.1f kWh", totalMonthCons),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ==========================================
                        // BALKENDIAGRAMM: VERBRAUCH JE INTERVALL
                        // Linke Skala: kWh | Untere Leiste: Datum
                        // ==========================================
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("analytics_chart_card"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        "Verbrauch je Intervall",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        "Erfasste Intervalle im $monthTitle",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    val primaryColor = MaterialTheme.colorScheme.primary
                                    val secondaryColor = MaterialTheme.colorScheme.secondary
                                    val axisColor = MaterialTheme.colorScheme.outline
                                    val textColorInt = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
                                    val primaryColorInt = primaryColor.toArgb()
                                    val secondaryColorInt = secondaryColor.toArgb()
                                    val density = LocalDensity.current

                                    val maxVal = chartIntervals.maxOfOrNull {
                                        maxOf(it.normalConsumption, it.heatPumpConsumption)
                                    } ?: 10.0
                                    val maxY = calculateNiceMax(maxVal)

                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(220.dp)
                                    ) {
                                        val canvasWidth = size.width
                                        val canvasHeight = size.height

                                        val leftPadding = with(density) { 54.dp.toPx() }
                                        val rightPadding = with(density) { 14.dp.toPx() }
                                        val topPadding = with(density) { 26.dp.toPx() }
                                        val bottomPadding = with(density) { 38.dp.toPx() }

                                        val graphWidth = canvasWidth - leftPadding - rightPadding
                                        val graphHeight = canvasHeight - topPadding - bottomPadding
                                        val baselineY = canvasHeight - bottomPadding

                                        // Paints
                                        val scalePaint = Paint().apply {
                                            color = textColorInt
                                            textSize = with(density) { 11.sp.toPx() }
                                            textAlign = Paint.Align.RIGHT
                                            isAntiAlias = true
                                        }

                                        val unitPaint = Paint().apply {
                                            color = textColorInt
                                            textSize = with(density) { 12.sp.toPx() }
                                            typeface = Typeface.DEFAULT_BOLD
                                            textAlign = Paint.Align.RIGHT
                                            isAntiAlias = true
                                        }

                                        val datePaint = Paint().apply {
                                            color = textColorInt
                                            textSize = with(density) { 11.sp.toPx() }
                                            textAlign = Paint.Align.CENTER
                                            isAntiAlias = true
                                        }

                                        val barValuePaintNormal = Paint().apply {
                                            color = primaryColorInt
                                            textSize = with(density) { 9.sp.toPx() }
                                            typeface = Typeface.DEFAULT_BOLD
                                            textAlign = Paint.Align.CENTER
                                            isAntiAlias = true
                                        }

                                        val barValuePaintHp = Paint().apply {
                                            color = secondaryColorInt
                                            textSize = with(density) { 9.sp.toPx() }
                                            typeface = Typeface.DEFAULT_BOLD
                                            textAlign = Paint.Align.CENTER
                                            isAntiAlias = true
                                        }

                                        // 1. LINKE SKALA & HORIZONTALE GITTERLINIEN (kWh)
                                        val steps = 4
                                        for (i in 0..steps) {
                                            val fraction = i / steps.toFloat()
                                            val tickValue = maxY * fraction
                                            val y = baselineY - (graphHeight * fraction)

                                            // Gitterlinie
                                            drawLine(
                                                color = Color.LightGray.copy(alpha = 0.4f),
                                                start = Offset(leftPadding, y),
                                                end = Offset(canvasWidth - rightPadding, y),
                                                strokeWidth = 1f
                                            )

                                            // Skalenbeschriftung links
                                            val tickLabel = if (maxY < 10 && tickValue % 1.0 != 0.0) {
                                                String.format(Locale.GERMAN, "%.1f", tickValue)
                                            } else {
                                                String.format(Locale.GERMAN, "%.0f", tickValue)
                                            }
                                            drawContext.canvas.nativeCanvas.drawText(
                                                tickLabel,
                                                leftPadding - 8f,
                                                y + with(density) { 4.dp.toPx() },
                                                scalePaint
                                            )
                                        }

                                        // Einheit "kWh" oben über der Skala
                                        drawContext.canvas.nativeCanvas.drawText(
                                            "kWh",
                                            leftPadding - 8f,
                                            topPadding - with(density) { 8.dp.toPx() },
                                            unitPaint
                                        )

                                        // 2. UNTERE ACHSENLINIE (Basislinie)
                                        drawLine(
                                            color = axisColor.copy(alpha = 0.7f),
                                            start = Offset(leftPadding, baselineY),
                                            end = Offset(canvasWidth - rightPadding, baselineY),
                                            strokeWidth = 2f
                                        )

                                        // 3. BALKEN & UNTERE DATUMSANZEIGE JE INTERVALL
                                        val count = chartIntervals.size
                                        if (count > 0) {
                                            val slotWidth = graphWidth / count
                                            val maxBarWidth = with(density) { 20.dp.toPx() }
                                            val barSpacing = with(density) { 3.dp.toPx() }
                                            val barWidth = ((slotWidth - with(density) { 16.dp.toPx() }) / 2f)
                                                .coerceIn(with(density) { 7.dp.toPx() }, maxBarWidth)

                                            chartIntervals.forEachIndexed { index, report ->
                                                val groupCenterX = leftPadding + (index + 0.5f) * slotWidth
                                                val normalLeft = groupCenterX - barWidth - (barSpacing / 2f)
                                                val hpLeft = groupCenterX + (barSpacing / 2f)

                                                val normalHeight = ((report.normalConsumption / maxY) * graphHeight)
                                                    .toFloat()
                                                    .coerceAtLeast(0f)
                                                val hpHeight = ((report.heatPumpConsumption / maxY) * graphHeight)
                                                    .toFloat()
                                                    .coerceAtLeast(0f)

                                                val normalTop = baselineY - normalHeight
                                                val hpTop = baselineY - hpHeight

                                                // Tag-Strom Balken
                                                if (normalHeight > 0f) {
                                                    drawRoundRect(
                                                        color = primaryColor,
                                                        topLeft = Offset(normalLeft, normalTop),
                                                        size = Size(barWidth, normalHeight),
                                                        cornerRadius = CornerRadius(4f, 4f)
                                                    )
                                                }

                                                // Wärmepumpe Balken
                                                if (hpHeight > 0f) {
                                                    drawRoundRect(
                                                        color = secondaryColor,
                                                        topLeft = Offset(hpLeft, hpTop),
                                                        size = Size(barWidth, hpHeight),
                                                        cornerRadius = CornerRadius(4f, 4f)
                                                    )
                                                }

                                                // Zahlenwerte über den Balken (falls Platz)
                                                if (normalHeight > 10f) {
                                                    val nLabel = if (report.normalConsumption % 1.0 == 0.0) {
                                                        "${report.normalConsumption.toLong()}"
                                                    } else {
                                                        String.format(Locale.GERMAN, "%.1f", report.normalConsumption)
                                                    }
                                                    drawContext.canvas.nativeCanvas.drawText(
                                                        nLabel,
                                                        normalLeft + barWidth / 2f,
                                                        (normalTop - 5f).coerceAtLeast(topPadding),
                                                        barValuePaintNormal
                                                    )
                                                }

                                                if (hpHeight > 10f) {
                                                    val hpLabel = if (report.heatPumpConsumption % 1.0 == 0.0) {
                                                        "${report.heatPumpConsumption.toLong()}"
                                                    } else {
                                                        String.format(Locale.GERMAN, "%.1f", report.heatPumpConsumption)
                                                    }
                                                    drawContext.canvas.nativeCanvas.drawText(
                                                        hpLabel,
                                                        hpLeft + barWidth / 2f,
                                                        (hpTop - 5f).coerceAtLeast(topPadding),
                                                        barValuePaintHp
                                                    )
                                                }

                                                // Tick unter dem Intervall
                                                drawLine(
                                                    color = axisColor.copy(alpha = 0.7f),
                                                    start = Offset(groupCenterX, baselineY),
                                                    end = Offset(groupCenterX, baselineY + 5f),
                                                    strokeWidth = 1.5f
                                                )

                                                // Untere Datumsanzeige
                                                val startGerman = com.example.utils.DateUtils.formatToGermanDate(report.startString)
                                                val endGerman = com.example.utils.DateUtils.formatToGermanDate(report.endString)
                                                val startDay = if (startGerman.length >= 5) startGerman.take(5) else startGerman
                                                val endDay = if (endGerman.length >= 5) endGerman.take(5) else endGerman

                                                val dateText = if (count <= 3 && startDay.isNotBlank() && startDay != endDay) {
                                                    "${startDay.take(2)}.-$endDay"
                                                } else {
                                                    endDay
                                                }

                                                drawContext.canvas.nativeCanvas.drawText(
                                                    dateText,
                                                    groupCenterX,
                                                    baselineY + with(density) { 18.dp.toPx() },
                                                    datePaint
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Legende
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(primaryColor, shape = RoundedCornerShape(2.dp))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Tag-Strom",
                                                color = primaryColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(24.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(secondaryColor, shape = RoundedCornerShape(2.dp))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Wärmepumpe",
                                                color = secondaryColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ==========================================
                        // DETAIL-LISTE DER INTERVALLE IM MONAT
                        // ==========================================
                        item {
                            Text(
                                "Erfasste Intervalle im $monthTitle",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(monthIntervals) { report ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    val repStart = com.example.utils.DateUtils.formatToGermanDate(report.startString)
                                    val repEnd = com.example.utils.DateUtils.formatToGermanDate(report.endString)
                                    Text(
                                        "$repStart bis $repEnd (${report.days} Tage)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Tag-Strom:",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.width(105.dp)
                                            )
                                            Text(
                                                String.format(Locale.GERMAN, "%.1f kWh", report.normalConsumption),
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                String.format(Locale.GERMAN, "%.2f €", report.normalTotalCost),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Wärmepumpe:",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.width(105.dp)
                                            )
                                            Text(
                                                String.format(Locale.GERMAN, "%.1f kWh", report.heatPumpConsumption),
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                String.format(Locale.GERMAN, "%.2f €", report.heatPumpTotalCost),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                    if (report.note.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "Notiz: ${report.note}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Gesamt: ${String.format(Locale.GERMAN, "%.1f kWh", report.totalConsumption)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            "Kosten: ${String.format(Locale.GERMAN, "%.2f €", report.totalCost)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
