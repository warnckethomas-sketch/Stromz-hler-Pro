package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.R
import com.example.data.HeatingGuideData
import com.example.data.HeatingMode
import com.example.data.HeatingSettingsEntity
import com.example.data.HeatingTimeSlot
import com.example.data.IntervalReport
import com.example.data.MeterReadingEntity
import com.example.data.OgHeatingProgram
import com.example.data.OgProgramCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatingGuideScreen(
    savedSettings: List<HeatingSettingsEntity> = emptyList(),
    readings: List<MeterReadingEntity> = emptyList(),
    intervals: List<IntervalReport> = emptyList(),
    onSaveSettings: (HeatingSettingsEntity) -> Unit = {},
    onDeleteSettings: (Long) -> Unit = {},
    onNavigateBack: () -> Unit,
    onExportPdf: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val tabs = listOf(
        "EG · ACALOR",
        "OG · Thermostat",
        "Einstellungen"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.heating_guide_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onExportPdf,
                        modifier = Modifier.testTag("heating_pdf_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = stringResource(R.string.heating_guide_export_pdf)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
            ) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag(
                                    when (index) {
                                        0 -> "tab_eg_acalor"
                                        1 -> "tab_og_thermostat"
                                        else -> "tab_heating_settings"
                                    }
                                ),
                            text = {
                                Text(
                                    text = title,
                                    maxLines = 1,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }

                when (selectedTabIndex) {
                    0 -> EgAcalorGuideContent()
                    1 -> OgThermostatGuideContent()
                    else -> HeatingSettingsEditor(
                        savedSettings = savedSettings,
                        readings = readings,
                        intervals = intervals,
                        onSaveSettings = { entity ->
                            onSaveSettings(entity)
                            scope.launch {
                                snackbarHostState.showSnackbar("Heizungseinstellung gespeichert")
                            }
                        },
                        onDeleteSettings = onDeleteSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun EgAcalorGuideContent() {
    var coolingExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Device Card
        item {
            val primaryColor = MaterialTheme.colorScheme.primary
            val tertiaryColor = MaterialTheme.colorScheme.tertiary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.16f),
                                    tertiaryColor.copy(alpha = 0.12f)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Thermostat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Erdgeschoss (EG)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ACALOR Klimaregler KTRRUU-212.170",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Kompaktanleitung für Heiz- & Kühlzeiten",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Step-by-step programming guide
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "1. Tagesprogramm für Heizphasen einstellen",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    GuideStepRow(
                        stepNumber = "1",
                        title = "Menü aufrufen",
                        description = "Drücken Sie die Menü-Taste (M / drei Linien). Wählen Sie mit den Pfeiltasten 'Tagesprogramm Heizen' und bestätigen Sie mit OK."
                    )
                    GuideStepRow(
                        stepNumber = "2",
                        title = "Tag wählen",
                        description = "Wählen Sie den gewünschten Wochentag (z. B. Mo) oder 'All' für alle Tage und drücken Sie OK."
                    )
                    GuideStepRow(
                        stepNumber = "3",
                        title = "Zeitleiste programmieren",
                        description = "Der Regler startet bei 00:00 Uhr. Nutzen Sie die Tasten, um Stunde für Stunde den Modus festzulegen. Nach 24:00 Uhr mit OK speichern."
                    )
                }
            }
        }

        // Visual 24h Recommended Schedule
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Empfohlenes Zeitschema (Morgens & Abends heizen)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Legend
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendPill(
                            color = Color(0xFFEA580C),
                            icon = Icons.Default.WbSunny,
                            label = "Komfort 21 °C (Plus/Balken)"
                        )
                        LegendPill(
                            color = Color(0xFF2563EB),
                            icon = Icons.Default.NightsStay,
                            label = "ECO 17 °C (Minus/Leer)"
                        )
                    }

                    // 24h Timeline Canvas
                    AcalorTimelineBar(slots = HeatingGuideData.acalorSchedule)

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    HeatingGuideData.acalorSchedule.forEachIndexed { idx, slot ->
                        AcalorSlotRow(slot = slot)
                        if (idx < HeatingGuideData.acalorSchedule.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        // Expandable Summer Cooling Mode
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable { coolingExpanded = !coolingExpanded }
                            .padding(16.dp)
                            .testTag("cooling_section_toggle"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AcUnit,
                                contentDescription = null,
                                tint = Color(0xFF0284C7)
                            )
                            Column {
                                Text(
                                    text = "2. Umschalten auf Kühlbetrieb im Sommer",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Manuelle Umstellung & separates Kühlprogramm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = if (coolingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (coolingExpanded) "Einklappen" else "Ausklappen"
                        )
                    }

                    AnimatedVisibility(
                        visible = coolingExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Sollte die automatische Umschaltung nicht aktiv sein, können Sie das System manuell auf Kühlung umstellen:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            GuideStepRow(
                                stepNumber = "1",
                                title = "Betriebsart öffnen",
                                description = "Menü-Taste drücken und zum Punkt 'Betriebsart' oder 'Modus' navigieren."
                            )
                            GuideStepRow(
                                stepNumber = "2",
                                title = "Auf Kühlen umstellen",
                                description = "Einstellung von 'Heizen' auf 'Kühlen' (Schneeflocken-Symbol) ändern und mit OK bestätigen."
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Wichtig: Für den Kühlbetrieb gelten separate Temperaturvorgaben (z. B. Komfortkühlung auf 24 °C), die analog über den Menüpunkt 'Tagesprogramm Kühlen' zeitlich definiert werden können.",
                                        style = MaterialTheme.typography.bodySmall
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

@Composable
private fun OgThermostatGuideContent() {
    var selectedCategory by remember { mutableStateOf(OgProgramCategory.ALL) }

    val filteredPrograms = remember(selectedCategory) {
        if (selectedCategory == OgProgramCategory.ALL) {
            HeatingGuideData.ogPrograms
        } else {
            HeatingGuideData.ogPrograms.filter { it.category == selectedCategory }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Setpoints Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Obergeschoss (OG) · Aktuelle Einstellungen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SetpointBadge(
                            label = "Heizen",
                            value = "18,5 °C",
                            subLabel = "momentan",
                            accentColor = Color(0xFFEA580C),
                            modifier = Modifier.weight(1f)
                        )
                        SetpointBadge(
                            label = "Abgesenkt",
                            value = "14 °C",
                            subLabel = "momentan",
                            accentColor = Color(0xFF2563EB),
                            modifier = Modifier.weight(1f)
                        )
                        SetpointBadge(
                            label = "Frostschutz",
                            value = "5 °C",
                            subLabel = "nur P0",
                            accentColor = Color(0xFF0284C7),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "Alle Programme (1–20) wechseln immer zwischen Heizen (18,5 °C) und abgesenkter Temperatur (14 °C).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Sun-Button Quick Override Tip
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF7ED)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEA580C),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Kurzfristig aufheizen? (Sonne-Taste)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9A3412)
                        )
                        Text(
                            text = "Wenn in der abgesenkten Zeit geheizt werden soll, einfach die Taste mit der Sonne drücken. Dies bleibt bis zur nächsten Programmschaltung aktiv (ab der ohnehin geheizt wird) – danach läuft das eingestellte Programm automatisch weiter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7C2D12)
                        )
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OgProgramCategory.entries.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("og_filter_chip_${category.name.lowercase()}"),
                        label = { Text(category.label) }
                    )
                }
            }
        }

        // Program List P0 - P11
        items(filteredPrograms, key = { it.number }) { program ->
            OgProgramCard(program = program)
        }

        // Warning for P12 - P20
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Hinweis zu Programm 12–20",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Programme 12–20 sind nicht eingestellt! Nicht benutzen oder erst individuell einstellen – sonst läuft die Heizung 24 Std. durch wie bei Programm 8.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideStepRow(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNumber,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendPill(
    color: Color,
    icon: ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AcalorTimelineBar(slots: List<HeatingTimeSlot>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            val totalHours = 24f
            slots.forEach { slot ->
                val startX = (slot.startHour / totalHours) * size.width
                val slotWidth = ((slot.endHour - slot.startHour) / totalHours) * size.width
                val slotColor = when (slot.mode) {
                    HeatingMode.COMFORT -> Color(0xFFEA580C)
                    HeatingMode.ECO -> Color(0xFF3B82F6)
                    HeatingMode.FROST_PROTECTION -> Color(0xFF0284C7)
                }
                drawRect(
                    color = slotColor,
                    topLeft = Offset(startX, 0f),
                    size = Size(slotWidth, size.height)
                )
            }

            // Hour tick dividers at 06, 12, 18
            listOf(6f, 12f, 18f).forEach { hour ->
                val x = (hour / totalHours) * size.width
                drawLine(
                    color = Color.White.copy(alpha = 0.6f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 2f
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("00:00", "06:00", "12:00", "18:00", "24:00").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun AcalorSlotRow(slot: HeatingTimeSlot) {
    val isComfort = slot.mode == HeatingMode.COMFORT
    val accentColor = if (isComfort) Color(0xFFEA580C) else Color(0xFF2563EB)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1.2f)) {
            Text(
                text = slot.timeLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = slot.displaySymbol,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = accentColor.copy(alpha = 0.14f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isComfort) Icons.Default.WbSunny else Icons.Default.NightsStay,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${slot.modeName} (${slot.temperature})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
private fun SetpointBadge(
    label: String,
    value: String,
    subLabel: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun OgProgramCard(program: OgHeatingProgram) {
    val baseColor = when (program.mode) {
        HeatingMode.COMFORT -> Color(0xFFBFDBFE)
        HeatingMode.ECO -> Color(0xFF93C5FD)
        HeatingMode.FROST_PROTECTION -> Color(0xFFBAE6FD)
    }
    val activeColor = Color(0xFFEA580C)
    val badgeBg = when (program.mode) {
        HeatingMode.FROST_PROTECTION -> Color(0xFF0284C7)
        HeatingMode.ECO -> Color(0xFF2563EB)
        HeatingMode.COMFORT -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = "P${program.number}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = program.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            // Proportional 24h mini-timeline
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
            ) {
                drawRoundRect(
                    color = baseColor,
                    cornerRadius = CornerRadius(5f, 5f)
                )
                program.activeRanges.forEach { (start, end) ->
                    val startX = (start / 24f) * size.width
                    val width = ((end - start) / 24f) * size.width
                    drawRect(
                        color = activeColor,
                        topLeft = Offset(startX, 0f),
                        size = Size(width, size.height)
                    )
                }
            }
        }
    }
}
