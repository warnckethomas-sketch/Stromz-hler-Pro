package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IntervalReport
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    intervals: List<IntervalReport>,
    onNavigateBack: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val textColorInt = android.graphics.Color.GRAY

    // Take up to last 6 intervals for the 6-month trend view
    val last6Intervals = intervals.takeLast(6)

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Verbrauchs- und Kostenübersicht",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (intervals.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Keine Intervalldaten für die Auswertung vorhanden.")
                        }
                    }
                }
            } else {
                // ==========================================
                // LINIENDIAGRAMM-WIDGET (Letzte 6 Monate)
                // ==========================================
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("line_chart_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text("Verbrauchstrend (Tag-Strom & Wärmepumpe)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Vergleich der letzten Ablesezeiträume", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))

                            val maxConsumption = last6Intervals.maxOfOrNull { maxOf(it.normalConsumption, it.heatPumpConsumption) } ?: 100.0

                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            ) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                val leftPadding = 40f
                                val rightPadding = 20f
                                val topPadding = 20f
                                val bottomPadding = 40f
                                val graphWidth = canvasWidth - leftPadding - rightPadding
                                val graphHeight = canvasHeight - topPadding - bottomPadding

                                val dataPointsCount = last6Intervals.size
                                val stepX = if (dataPointsCount > 1) graphWidth / (dataPointsCount - 1) else graphWidth

                                // Draw horizontal grid lines
                                val gridLines = 4
                                for (i in 0..gridLines) {
                                    val y = topPadding + (graphHeight / gridLines) * i
                                    drawLine(
                                        color = Color.LightGray.copy(alpha = 0.5f),
                                        start = Offset(leftPadding, y),
                                        end = Offset(canvasWidth - rightPadding, y),
                                        strokeWidth = 1f
                                    )
                                }

                                if (dataPointsCount > 0) {
                                    val normalPath = Path()
                                    val hpPath = Path()

                                    last6Intervals.forEachIndexed { index, report ->
                                        val x = leftPadding + if (dataPointsCount > 1) index * stepX else graphWidth / 2f
                                        val normalRatio = (report.normalConsumption / maxConsumption).toFloat()
                                        val hpRatio = (report.heatPumpConsumption / maxConsumption).toFloat()

                                        val normalY = canvasHeight - bottomPadding - (graphHeight * normalRatio)
                                        val hpY = canvasHeight - bottomPadding - (graphHeight * hpRatio)

                                        if (index == 0) {
                                            normalPath.moveTo(x, normalY)
                                            hpPath.moveTo(x, hpY)
                                        } else {
                                            normalPath.lineTo(x, normalY)
                                            hpPath.lineTo(x, hpY)
                                        }

                                        // Draw data points (circles)
                                        drawCircle(
                                            color = primaryColor,
                                            radius = 5f,
                                            center = Offset(x, normalY)
                                        )
                                        drawCircle(
                                            color = secondaryColor,
                                            radius = 5f,
                                            center = Offset(x, hpY)
                                        )

                                        // Draw X axis labels (short date: dd.MM.)
                                        val germanDate = com.example.utils.DateUtils.formatToGermanDate(report.endString)
                                        val label = if (germanDate.length >= 6) germanDate.take(5) else germanDate
                                        drawContext.canvas.nativeCanvas.drawText(
                                            label,
                                            x - 20f,
                                            canvasHeight - bottomPadding + 20f,
                                            android.graphics.Paint().apply {
                                                color = textColorInt
                                                textSize = 28f
                                            }
                                        )
                                    }

                                    // Draw lines
                                    drawPath(
                                        path = normalPath,
                                        color = primaryColor,
                                        style = Stroke(width = 3f)
                                    )

                                    drawPath(
                                        path = hpPath,
                                        color = secondaryColor,
                                        style = Stroke(width = 3f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("■ Tag-Strom", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.padding(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("■ Wärmepumpe", color = secondaryColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Bar Chart Card (Intervallvergleich)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analytics_chart_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text("Verbrauch (kWh) je Intervall (Balken)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            val maxConsumption = intervals.maxOfOrNull { it.totalConsumption } ?: 1.0

                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                val barWidth = (canvasWidth / (intervals.size * 2f)).coerceIn(15f, 60f)
                                val spacing = barWidth

                                var x = 40f
                                val topPadding = 20f
                                val bottomPadding = 30f
                                val graphHeight = canvasHeight - topPadding - bottomPadding

                                intervals.reversed().forEach { report ->
                                    val normalRatio = (report.normalConsumption / maxConsumption).toFloat()
                                    val hpRatio = (report.heatPumpConsumption / maxConsumption).toFloat()

                                    val normalHeight = graphHeight * normalRatio
                                    val hpHeight = graphHeight * hpRatio

                                    // Normal bar
                                    drawRect(
                                        color = primaryColor,
                                        topLeft = Offset(x, canvasHeight - bottomPadding - normalHeight),
                                        size = Size(barWidth, normalHeight)
                                    )

                                    // Heat Pump bar stacked on side
                                    drawRect(
                                        color = secondaryColor,
                                        topLeft = Offset(x + barWidth + 4f, canvasHeight - bottomPadding - hpHeight),
                                        size = Size(barWidth, hpHeight)
                                    )

                                    x += (barWidth * 2) + spacing
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("■ Tag-Strom", color = primaryColor, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.padding(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("■ Wärmepumpe", color = secondaryColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Summary stats per interval
                item {
                    Text("Details je Intervall", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                items(intervals) { report ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            val repStart = com.example.utils.DateUtils.formatToGermanDate(report.startString)
                            val repEnd = com.example.utils.DateUtils.formatToGermanDate(report.endString)
                            Text("$repStart bis $repEnd (${report.days} Tage)", fontWeight = FontWeight.Bold)
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

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Gesamt: ${report.totalConsumption} kWh", fontWeight = FontWeight.Bold)
                                Text("Kosten: ${String.format(Locale.GERMAN, "%.2f €", report.totalCost)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
