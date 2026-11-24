package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HeatingGuideData
import com.example.data.HeatingSettingsEntity
import com.example.data.IntervalReport
import com.example.data.MeterReadingEntity
import com.example.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatingSettingsEditor(
    savedSettings: List<HeatingSettingsEntity>,
    readings: List<MeterReadingEntity>,
    intervals: List<IntervalReport>,
    onSaveSettings: (HeatingSettingsEntity) -> Unit,
    onDeleteSettings: (Long) -> Unit
) {
    val context = LocalContext.current
    val latest = savedSettings.firstOrNull()

    // Effective Date state
    val todayIso = remember { DateUtils.timestampToIsoDateString(System.currentTimeMillis()) }
    var effectiveIso by remember(latest?.id) {
        mutableStateOf(
            latest?.effectiveDateString?.let { DateUtils.toIsoDateString(it) }?.ifBlank { todayIso } ?: todayIso
        )
    }
    var effectiveTimestamp by remember(latest?.id) {
        mutableStateOf(latest?.effectiveTimestamp ?: System.currentTimeMillis())
    }
    var dateDropdownExpanded by remember { mutableStateOf(false) }

    // EG State
    var egMode by remember(latest?.id) { mutableStateOf(latest?.egMode ?: "HEIZEN") }
    var egComfortTemp by remember(latest?.id) { mutableDoubleStateOf(latest?.egComfortTemp ?: 21.0) }
    var egEcoTemp by remember(latest?.id) { mutableDoubleStateOf(latest?.egEcoTemp ?: 17.0) }
    var egCoolTemp by remember(latest?.id) { mutableDoubleStateOf(latest?.egCoolTemp ?: 24.0) }
    var egScheduleType by remember(latest?.id) { mutableStateOf(latest?.egScheduleType ?: "ALL") }
    var egAllMask by remember(latest?.id) {
        mutableStateOf(HeatingGuideData.parseEgMask(latest?.egAllDaysMask ?: HeatingGuideData.DEFAULT_EG_MASK))
    }
    var egWeeklyMasks by remember(latest?.id) {
        mutableStateOf(HeatingGuideData.parseWeeklyMasks(latest?.egWeeklyMasks ?: ""))
    }
    var selectedEgDayIndex by remember { mutableIntStateOf(0) }

    // OG State
    var ogHeatTemp by remember(latest?.id) { mutableDoubleStateOf(latest?.ogHeatTemp ?: 18.5) }
    var ogSetbackTemp by remember(latest?.id) { mutableDoubleStateOf(latest?.ogSetbackTemp ?: 14.0) }
    var ogFrostTemp by remember(latest?.id) { mutableDoubleStateOf(latest?.ogFrostTemp ?: 5.0) }
    var ogDayPrograms by remember(latest?.id) {
        mutableStateOf(HeatingGuideData.parseOgDayPrograms(latest?.ogDayPrograms ?: "1,1,1,1,1,1,1"))
    }

    val loadIntoEditor: (HeatingSettingsEntity) -> Unit = { item ->
        effectiveIso = DateUtils.toIsoDateString(item.effectiveDateString).ifBlank { todayIso }
        effectiveTimestamp = item.effectiveTimestamp
        egMode = item.egMode
        egComfortTemp = item.egComfortTemp
        egEcoTemp = item.egEcoTemp
        egCoolTemp = item.egCoolTemp
        egScheduleType = item.egScheduleType
        egAllMask = HeatingGuideData.parseEgMask(item.egAllDaysMask)
        egWeeklyMasks = HeatingGuideData.parseWeeklyMasks(item.egWeeklyMasks)
        ogHeatTemp = item.ogHeatTemp
        ogSetbackTemp = item.ogSetbackTemp
        ogFrostTemp = item.ogFrostTemp
        ogDayPrograms = HeatingGuideData.parseOgDayPrograms(item.ogDayPrograms)
    }

    val openCalendarPicker: () -> Unit = {
        val cal = Calendar.getInstance()
        try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN).parse(effectiveIso)
            if (parsed != null) cal.time = parsed
        } catch (_: Exception) {}
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth, 12, 0, 0)
                effectiveTimestamp = cal.timeInMillis
                effectiveIso = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN).format(cal.time)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Quick date options for the dropdown
    val datePresets = remember(readings) {
        val list = mutableListOf<Pair<String, Pair<String, Long>>>()
        val cal = Calendar.getInstance()
        val todayMs = cal.timeInMillis
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN).format(Date(todayMs))
        list.add("Heute (${DateUtils.formatToGermanDate(todayStr)})" to (todayStr to todayMs))

        cal.add(Calendar.DAY_OF_MONTH, -1)
        val yestMs = cal.timeInMillis
        val yestStr = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN).format(Date(yestMs))
        list.add("Gestern (${DateUtils.formatToGermanDate(yestStr)})" to (yestStr to yestMs))

        readings.sortedByDescending { it.date }.take(3).forEach { r ->
            val iso = DateUtils.toIsoDateString(r.dateString).ifBlank { DateUtils.timestampToIsoDateString(r.date) }
            if (list.none { it.second.first == iso }) {
                list.add("Ablesung ${DateUtils.formatToGermanDate(iso)}" to (iso to r.date))
            }
        }
        list
    }

    // Determine matching interval hint
    val matchedIntervalText = remember(effectiveIso, intervals) {
        val sortedIntervals = intervals.sortedBy { it.startDate }
        val match = sortedIntervals.findIndexedMatch(effectiveIso)
        if (match != null) {
            "Wird als Extra-Notiz im Zeitraum ${match.startString} → ${match.endString} angezeigt."
        } else {
            val latestEnd = sortedIntervals.lastOrNull()?.endString
            if (latestEnd != null) {
                "Gilt ab ${DateUtils.formatToGermanDate(effectiveIso)} (erscheint beim nächsten Ablesezeitraum ab $latestEnd)."
            } else {
                "Wird als Extra-Notiz zugeordnet, sobald Ablesezeiträume vorhanden sind."
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Einstelldatum Dropdown Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Einstelldatum wählen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    ExposedDropdownMenuBox(
                        expanded = dateDropdownExpanded,
                        onExpandedChange = { dateDropdownExpanded = !dateDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = DateUtils.formatToGermanDate(effectiveIso),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Einstelldatum") },
                            leadingIcon = {
                                Icon(Icons.Default.CalendarToday, contentDescription = null)
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dateDropdownExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                                .testTag("heating_date_dropdown")
                        )

                        ExposedDropdownMenu(
                            expanded = dateDropdownExpanded,
                            onDismissRequest = { dateDropdownExpanded = false }
                        ) {
                            datePresets.forEach { (label, pair) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        effectiveIso = pair.first
                                        effectiveTimestamp = pair.second
                                        dateDropdownExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Anderes Datum im Kalender wählen…",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    dateDropdownExpanded = false
                                    openCalendarPicker()
                                }
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = matchedIntervalText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 2. EG - ACALOR Settings Card
        item {
            val activeHours = if (egScheduleType == "ALL") {
                egAllMask
            } else {
                egWeeklyMasks.getOrElse(selectedEgDayIndex) { HeatingGuideData.parseEgMask(HeatingGuideData.DEFAULT_EG_MASK) }
            }

            val updateActiveHours: (List<Boolean>) -> Unit = { updated ->
                if (egScheduleType == "ALL") {
                    egAllMask = updated
                } else {
                    egWeeklyMasks = egWeeklyMasks.mapIndexed { idx, list ->
                        if (idx == selectedEgDayIndex) updated else list
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "EG · ACALOR KTRRUU-212.170",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Betriebsart
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Betriebsart:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        FilterChip(
                            selected = egMode == "HEIZEN",
                            onClick = { egMode = "HEIZEN" },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                            label = { Text("Heizen") }
                        )
                        FilterChip(
                            selected = egMode == "KUEHLEN",
                            onClick = { egMode = "KUEHLEN" },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                            label = { Text("Kühlen") }
                        )
                    }

                    // Temperatures
                    if (egMode == "HEIZEN") {
                        TemperatureStepperRow(
                            label = "Komfort-Temperatur",
                            value = egComfortTemp,
                            min = 10.0,
                            max = 30.0,
                            accentColor = Color(0xFFEA580C),
                            onValueChange = { egComfortTemp = it }
                        )
                        TemperatureStepperRow(
                            label = "ECO-Temperatur",
                            value = egEcoTemp,
                            min = 5.0,
                            max = 25.0,
                            accentColor = Color(0xFF2563EB),
                            onValueChange = { egEcoTemp = it }
                        )
                    } else {
                        TemperatureStepperRow(
                            label = "Komfortkühlung",
                            value = egCoolTemp,
                            min = 16.0,
                            max = 30.0,
                            accentColor = Color(0xFF0284C7),
                            onValueChange = { egCoolTemp = it }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Tag-Auswahl: "All" oder "Mo-So"
                    Text(
                        text = "Tag-Auswahl",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = egScheduleType == "ALL",
                            onClick = { egScheduleType = "ALL" },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("eg_schedule_all"),
                            label = { Text("All (Alle Tage)") }
                        )
                        FilterChip(
                            selected = egScheduleType == "WEEKLY",
                            onClick = { egScheduleType = "WEEKLY" },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("eg_schedule_weekly"),
                            label = { Text("Mo–So einzeln") }
                        )
                    }

                    if (egScheduleType == "WEEKLY") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HeatingGuideData.WEEKDAYS.forEachIndexed { idx, day ->
                                FilterChip(
                                    selected = selectedEgDayIndex == idx,
                                    onClick = { selectedEgDayIndex = idx },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                    label = { Text(day) }
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                val current = egWeeklyMasks[selectedEgDayIndex]
                                egWeeklyMasks = List(7) { current }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${HeatingGuideData.WEEKDAYS[selectedEgDayIndex]} auf alle Tage kopieren")
                        }
                    }

                    // 24-Stunden-Zeitleiste (wie am Thermostat)
                    Text(
                        text = "24-Stunden-Zeitleiste (antippen zum Umschalten):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(14.dp))
                            Text("Balken = Komfort (+)", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.NightsStay, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(14.dp))
                            Text("Leer = ECO (–)", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // 4 rows x 6 hours = 24 hours
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (rowIdx in 0 until 4) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (colIdx in 0 until 6) {
                                    val hour = rowIdx * 6 + colIdx
                                    val isComfort = activeHours.getOrNull(hour) == true
                                    val bgColor = if (isComfort) Color(0xFFEA580C) else Color(0xFFDBEAFE)
                                    val fgColor = if (isComfort) Color.White else Color(0xFF1E3A8A)
                                    val borderColor = if (isComfort) Color(0xFFC2410C) else Color(0xFF93C5FD)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bgColor)
                                            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                            .clickable {
                                                val mutable = activeHours.toMutableList()
                                                mutable[hour] = !isComfort
                                                updateActiveHours(mutable)
                                            }
                                            .testTag("eg_hour_$hour"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = String.format("%02d", hour),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = fgColor
                                            )
                                            Text(
                                                text = if (isComfort) "+" else "–",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = fgColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Quick presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = {
                            updateActiveHours(HeatingGuideData.parseEgMask(HeatingGuideData.DEFAULT_EG_MASK))
                        }) {
                            Text("Empfohlenes Schema", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = {
                            updateActiveHours(List(24) { false })
                        }) {
                            Text("Alles ECO", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = {
                            updateActiveHours(List(24) { true })
                        }) {
                            Text("Alles Komfort", fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = "Aktiv: ${HeatingGuideData.formatHourRanges(activeHours)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 3. OG - Raumthermostat Settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "OG · Raumthermostat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    TemperatureStepperRow(
                        label = "Heizen (Tag)",
                        value = ogHeatTemp,
                        min = 10.0,
                        max = 28.0,
                        accentColor = Color(0xFFEA580C),
                        onValueChange = { ogHeatTemp = it }
                    )
                    TemperatureStepperRow(
                        label = "Abgesenkt (Nacht/ECO)",
                        value = ogSetbackTemp,
                        min = 8.0,
                        max = 24.0,
                        accentColor = Color(0xFF2563EB),
                        onValueChange = { ogSetbackTemp = it }
                    )
                    TemperatureStepperRow(
                        label = "Frostschutz (nur P0)",
                        value = ogFrostTemp,
                        min = 3.0,
                        max = 12.0,
                        accentColor = Color(0xFF0284C7),
                        onValueChange = { ogFrostTemp = it }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Programm je Wochentag",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = {
                                val firstProg = ogDayPrograms.firstOrNull() ?: 1
                                ogDayPrograms = List(7) { firstProg }
                            }
                        ) {
                            Text("Mo auf alle", fontSize = 12.sp)
                        }
                    }

                    HeatingGuideData.WEEKDAYS.forEachIndexed { dayIdx, dayName ->
                        val selectedProg = ogDayPrograms.getOrElse(dayIdx) { 1 }
                        var menuExpanded by remember { mutableStateOf(false) }
                        val progInfo = HeatingGuideData.ogPrograms.find { it.number == selectedProg }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(34.dp)
                            )

                            Box(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { menuExpanded = true }
                                        .testTag("og_day_program_$dayIdx")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "P$selectedProg · ${progInfo?.summary ?: ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Programm wählen"
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    HeatingGuideData.ogPrograms.forEach { prog ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "P${prog.number} – ${prog.summary}",
                                                    fontWeight = if (prog.number == selectedProg) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                ogDayPrograms = ogDayPrograms.mapIndexed { idx, old ->
                                                    if (idx == dayIdx) prog.number else old
                                                }
                                                menuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Save Button
        item {
            Button(
                onClick = {
                    val entity = HeatingSettingsEntity(
                        effectiveTimestamp = effectiveTimestamp,
                        effectiveDateString = effectiveIso,
                        egMode = egMode,
                        egComfortTemp = egComfortTemp,
                        egEcoTemp = egEcoTemp,
                        egCoolTemp = egCoolTemp,
                        egScheduleType = egScheduleType,
                        egAllDaysMask = HeatingGuideData.encodeEgMask(egAllMask),
                        egWeeklyMasks = HeatingGuideData.encodeWeeklyMasks(egWeeklyMasks),
                        ogHeatTemp = ogHeatTemp,
                        ogSetbackTemp = ogSetbackTemp,
                        ogFrostTemp = ogFrostTemp,
                        ogDayPrograms = HeatingGuideData.encodeOgDayPrograms(ogDayPrograms),
                        updatedAt = System.currentTimeMillis()
                    )
                    onSaveSettings(entity)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_heating_settings_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Einstellung für ${DateUtils.formatToGermanDate(effectiveIso)} speichern",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 5. History of saved settings
        if (savedSettings.isNotEmpty()) {
            item {
                Text(
                    text = "Gespeicherte Heizungseinstellungen (${savedSettings.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(savedSettings, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ab ${DateUtils.formatToGermanDate(item.effectiveDateString)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row {
                                IconButton(
                                    onClick = { loadIntoEditor(item) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "In Editor laden",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteSettings(item.id) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Einstellung löschen",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        Text(
                            text = HeatingGuideData.formatHeatingExtraNote(item),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemperatureStepperRow(
    label: String,
    value: Double,
    min: Double,
    max: Double,
    accentColor: Color,
    onValueChange: (Double) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = {
                    val next = (((value - 0.5) * 2.0).roundToInt() / 2.0).coerceAtLeast(min)
                    onValueChange(next)
                },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Default.Remove, contentDescription = "$label verringern")
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.14f)
            ) {
                Text(
                    text = String.format(Locale.GERMAN, "%.1f °C", value),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            IconButton(
                onClick = {
                    val next = (((value + 0.5) * 2.0).roundToInt() / 2.0).coerceAtMost(max)
                    onValueChange(next)
                },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Default.Add, contentDescription = "$label erhöhen")
            }
        }
    }
}

private fun List<IntervalReport>.findIndexedMatch(settingIso: String): IntervalReport? {
    for (idx in indices) {
        val interval = this[idx]
        val startIso = DateUtils.toIsoDateString(interval.startString)
        val endIso = DateUtils.toIsoDateString(interval.endString)
        val isLatest = (idx == lastIndex)
        val matches = if (isLatest) {
            settingIso >= startIso && settingIso <= endIso
        } else {
            settingIso >= startIso && settingIso < endIso
        }
        if (matches) return interval
    }
    return null
}
