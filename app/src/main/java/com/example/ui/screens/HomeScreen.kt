package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IntervalReport
import com.example.data.MeterReadingEntity
import com.example.data.TenantEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun extractMonthKey(dateStr: String): String {
    val trimmed = dateStr.trim()
    if (trimmed.matches(Regex("""\d{4}-\d{2}.*"""))) {
        val parts = trimmed.split("-")
        return "${parts[0]}-${parts[1]}"
    }
    if (trimmed.matches(Regex("""\d{2}\.\d{2}\.\d{4}.*"""))) {
        val parts = trimmed.split(".")
        return "${parts[2]}-${parts[1]}"
    }
    return "Sonstige"
}

fun formatMonthYear(yearMonth: String): String {
    val parts = yearMonth.split("-")
    if (parts.size != 2) return yearMonth
    val year = parts[0]
    val monthNum = parts[1].toIntOrNull() ?: return yearMonth
    val monthNames = arrayOf(
        "", "Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember"
    )
    val monthName = if (monthNum in 1..12) monthNames[monthNum] else parts[1]
    return "$monthName $year"
}

fun formatKwhValue(value: Double): String {
    return if (value % 1.0 == 0.0) {
        String.format(Locale.GERMAN, "%,d kWh", value.toLong())
    } else {
        String.format(Locale.GERMAN, "%,.1f kWh", value)
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    tenants: List<TenantEntity>,
    selectedTenant: TenantEntity?,
    readings: List<MeterReadingEntity>,
    intervals: List<IntervalReport>,
    isBackingUp: Boolean = false,
    onSelectTenant: (Long) -> Unit,
    onUpdateReadingTenant: (Long, Long) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToTariffs: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onExportPdf: (IntervalReport) -> Unit,
    onDeleteReading: (Long) -> Unit,
    onUpdateReading: ((MeterReadingEntity) -> Unit)? = null,
    onPerformExitBackup: suspend () -> Boolean = { true },
    onExitApp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestInterval = intervals.firstOrNull()

    var readingsSectionExpanded by remember { mutableStateOf(true) }
    var intervalsSectionExpanded by remember { mutableStateOf(true) }

    val expandedReadingsMonths = remember { mutableStateMapOf<String, Boolean>() }
    val expandedIntervalMonths = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(selectedTenant?.id) {
        expandedReadingsMonths.clear()
        expandedIntervalMonths.clear()
    }

    var tenantDropdownExpanded by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }
    var showExitSuccessDialog by remember { mutableStateOf(false) }

    var readingToAssign by remember { mutableStateOf<MeterReadingEntity?>(null) }
    var showAssignDialog by remember { mutableStateOf(false) }

    var readingBeingEdited by remember { mutableStateOf<MeterReadingEntity?>(null) }
    var showEditReadingDialog by remember { mutableStateOf(false) }
    var editNormalInput by remember { mutableStateOf("") }
    var editHpInput by remember { mutableStateOf("") }
    var editDateString by remember { mutableStateOf("") }
    var editTimestamp by remember { mutableStateOf(0L) }
    var editNoteInput by remember { mutableStateOf("") }
    var editInputError by remember { mutableStateOf<String?>(null) }

    val openEditDialog: (MeterReadingEntity) -> Unit = { reading ->
        readingBeingEdited = reading
        editNormalInput = if (reading.normalReading % 1.0 == 0.0) reading.normalReading.toInt().toString() else reading.normalReading.toString()
        editHpInput = if (reading.heatPumpReading % 1.0 == 0.0) reading.heatPumpReading.toInt().toString() else reading.heatPumpReading.toString()
        editDateString = com.example.utils.DateUtils.formatToGermanDate(reading.dateString)
        editTimestamp = reading.date
        editNoteInput = reading.note
        editInputError = null
        showEditReadingDialog = true
    }

    BackHandler {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("App beenden") },
            text = { Text("Möchten Sie die App wirklich beenden? Vor dem Schließen wird automatisch ein Sicherheits-Backup aller Daten erstellt.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        scope.launch {
                            isExiting = true
                            onPerformExitBackup()
                            isExiting = false
                            showExitSuccessDialog = true
                        }
                    }
                ) {
                    Text("Beenden & Sichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (showExitSuccessDialog) {
        LaunchedEffect(Unit) {
            delay(1200)
            showExitSuccessDialog = false
            onExitApp()
        }
        AlertDialog(
            onDismissRequest = {
                showExitSuccessDialog = false
                onExitApp()
            },
            icon = {
                Icon(
                    Icons.Default.Done,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Sicherung erfolgreich", fontWeight = FontWeight.Bold) },
            text = { Text("Alle Daten wurden erfolgreich gesichert. Die App schließt jetzt komplett...") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitSuccessDialog = false
                        onExitApp()
                    },
                    modifier = Modifier.testTag("confirm_exit_button")
                ) {
                    Text("Jetzt schließen")
                }
            }
        )
    }

    if (showAssignDialog && readingToAssign != null) {
        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            title = { Text("Zählerstand anderem Mieter zuordnen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val assignDate = com.example.utils.DateUtils.formatToGermanDate(readingToAssign!!.dateString)
                    Text("Wählen Sie den Mieter aus, dem dieser Zählerstand vom $assignDate zugeordnet werden soll:")
                    Spacer(modifier = Modifier.height(8.dp))
                    tenants.forEach { tenant ->
                        Button(
                            onClick = {
                                onUpdateReadingTenant(readingToAssign!!.id, tenant.id)
                                showAssignDialog = false
                                readingToAssign = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tenant.id == selectedTenant?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (tenant.id == selectedTenant?.id) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(tenant.name + if (!tenant.isActive) " (Archiviert)" else "")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAssignDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (showEditReadingDialog && readingBeingEdited != null) {
        val calendar = Calendar.getInstance().apply {
            if (editTimestamp > 0) timeInMillis = editTimestamp
        }
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                editTimestamp = calendar.timeInMillis
                editDateString = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN).format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        AlertDialog(
            onDismissRequest = { showEditReadingDialog = false },
            title = { Text("Zählerstand bearbeiten", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editDateString,
                        onValueChange = { editDateString = it },
                        label = { Text("Datum (TT.MM.JJJJ)") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Datum auswählen")
                            }
                        },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editNormalInput,
                        onValueChange = { editNormalInput = it },
                        label = { Text("Zählerstand Tag-Strom (kWh)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editHpInput,
                        onValueChange = { editHpInput = it },
                        label = { Text("Zählerstand Wärmepumpe (kWh)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editNoteInput,
                        onValueChange = { editNoteInput = it },
                        label = { Text("Notiz (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    if (editInputError != null) {
                        Text(
                            text = editInputError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val norm = editNormalInput.replace(',', '.').toDoubleOrNull()
                        val hp = editHpInput.replace(',', '.').toDoubleOrNull()
                        if (norm == null || hp == null) {
                            editInputError = "Bitte gültige Zählerstände eingeben."
                            return@Button
                        }
                        if (norm < 0 || hp < 0) {
                            editInputError = "Zählerstände können nicht negativ sein."
                            return@Button
                        }

                        val parsedDate = try {
                            SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN).parse(editDateString.trim())
                        } catch (e: Exception) {
                            null
                        }

                        val finalTimestamp = parsedDate?.time ?: editTimestamp
                        val finalDateString = if (parsedDate != null) {
                            SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN).format(parsedDate)
                        } else {
                            editDateString.trim()
                        }

                        val updated = readingBeingEdited!!.copy(
                            date = finalTimestamp,
                            dateString = finalDateString,
                            normalReading = norm,
                            heatPumpReading = hp,
                            note = editNoteInput.trim()
                        )
                        onUpdateReading?.invoke(updated)
                        showEditReadingDialog = false
                    }
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditReadingDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { tenantDropdownExpanded = true }
                    ) {
                        Column {
                            Text("StromZähler Pro", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    (selectedTenant?.name ?: "Kein Mieter") + if (selectedTenant?.isActive == false) " [Archiviert]" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Mieter wechseln")
                            }
                        }

                        DropdownMenu(
                            expanded = tenantDropdownExpanded,
                            onDismissRequest = { tenantDropdownExpanded = false }
                        ) {
                            Text(" Aktive Mieter", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            tenants.filter { it.isActive }.forEach { tenant ->
                                DropdownMenuItem(
                                    text = { Text(tenant.name + (if (tenant.id == selectedTenant?.id) " (Aktiv)" else "")) },
                                    onClick = {
                                        onSelectTenant(tenant.id)
                                        tenantDropdownExpanded = false
                                    }
                                )
                            }
                            val archived = tenants.filter { !it.isActive }
                            if (archived.isNotEmpty()) {
                                Text(" Archivierte Mieter", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                                archived.forEach { tenant ->
                                    val tStart = com.example.utils.DateUtils.formatToGermanDate(tenant.startDate)
                                    val tEnd = com.example.utils.DateUtils.formatToGermanDate(tenant.endDate)
                                    DropdownMenuItem(
                                        text = { Text("${tenant.name} ($tStart - $tEnd)") },
                                        onClick = {
                                            onSelectTenant(tenant.id)
                                            tenantDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (selectedTenant != null && selectedTenant.isActive) {
                        IconButton(
                            onClick = onNavigateToAdd,
                            modifier = Modifier.testTag("add_reading_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Neuer Zählerstand", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(
                        onClick = onNavigateToAnalytics,
                        modifier = Modifier.testTag("analytics_button")
                    ) {
                        Icon(Icons.Default.ElectricMeter, contentDescription = "Auswertung")
                    }
                    IconButton(
                        onClick = onNavigateToTariffs,
                        modifier = Modifier.testTag("tariffs_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Setup & Tarife")
                    }
                    IconButton(
                        onClick = { showExitDialog = true },
                        modifier = Modifier.testTag("exit_button")
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "App beenden & Backup", tint = MaterialTheme.colorScheme.error)
                    }
                }
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
            // =========================================================================
            // SICHERUNGSSTATUS - NUR ANZEIGEN WENN IN ARBEIT
            // =========================================================================
            if (isBackingUp || isExiting) {
                item(key = "backup_status") {
                    AnimatedVisibility(
                        visible = isBackingUp || isExiting,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("backup_status_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Sicherungsstatus: In Arbeit...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Latest / Current Period Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("latest_summary_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Letzter Ablesezeitraum",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (latestInterval != null) {
                                        val lStart = com.example.utils.DateUtils.formatToGermanDate(latestInterval.startString)
                                        val lEnd = com.example.utils.DateUtils.formatToGermanDate(latestInterval.endString)
                                        Text(
                                            "$lStart bis $lEnd (${latestInterval.days} Tage)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (latestInterval != null) {
                                IconButton(
                                    onClick = { onExportPdf(latestInterval) },
                                    modifier = Modifier.testTag("export_pdf_button")
                                ) {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = "PDF Export",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (latestInterval != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Tag-Strom", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        String.format(Locale.GERMAN, "%.1f kWh", latestInterval.normalConsumption),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        String.format(Locale.GERMAN, "%.2f €", latestInterval.normalTotalCost),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column {
                                    Text("Wärmepumpe", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        String.format(Locale.GERMAN, "%.1f kWh", latestInterval.heatPumpConsumption),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        String.format(Locale.GERMAN, "%.2f €", latestInterval.heatPumpTotalCost),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Gesamt", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        String.format(Locale.GERMAN, "%.1f kWh", latestInterval.totalConsumption),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        String.format(Locale.GERMAN, "%.2f €", latestInterval.totalCost),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        } else {
                            Text(
                                "Noch keine zwei Ablesungen vorhanden. Bitte erfassen Sie mindestens zwei Zählerstände, um den Verbrauch zu berechnen.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 1: Erfasste Zählerstände (Collapsible with Month Sub-folders)
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { readingsSectionExpanded = !readingsSectionExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ElectricMeter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Erfasste Zählerstände (${readings.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { readingsSectionExpanded = !readingsSectionExpanded }) {
                                Icon(
                                    if (readingsSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Ein-/Ausklappen"
                                )
                            }
                        }

                        if (readingsSectionExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            if (readings.isEmpty()) {
                                Text(
                                    "Keine Zählerstände erfasst für ${selectedTenant?.name ?: "diesen Mieter"}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            } else {
                                val readingsByMonth = readings.groupBy { reading ->
                                    extractMonthKey(reading.dateString)
                                }.toSortedMap(reverseOrder())

                                val currentCalendarMonth = SimpleDateFormat("yyyy-MM", Locale.GERMAN).format(java.util.Date())
                                val activeReadingsMonthKey = if (readingsByMonth.containsKey(currentCalendarMonth)) {
                                    currentCalendarMonth
                                } else {
                                    readingsByMonth.firstKey()
                                }

                                readingsByMonth.forEach { (yearMonth, monthReadings) ->
                                    val monthTitle = formatMonthYear(yearMonth)
                                    val defaultExpanded = if (readingsByMonth.size > 1) {
                                        yearMonth == activeReadingsMonthKey
                                    } else {
                                        true
                                    }
                                    val isMonthExpanded = expandedReadingsMonths[yearMonth] ?: defaultExpanded

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        // Month Folder Header
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .clickable {
                                                    expandedReadingsMonths[yearMonth] = !isMonthExpanded
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "$monthTitle (${monthReadings.size})",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            Icon(
                                                if (isMonthExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Month Readings Items
                                        if (isMonthExpanded) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            monthReadings.sortedByDescending { it.date }.forEach { reading ->
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .combinedClickable(
                                                            onClick = {},
                                                            onDoubleClick = { openEditDialog(reading) }
                                                        ),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(12.dp)
                                                    ) {
                                                        // Datum Kopfzeile
                                                        val readingDate = com.example.utils.DateUtils.formatToGermanDate(reading.dateString)
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                Icons.Default.DateRange,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                readingDate,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 15.sp,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        // Messungen als Kacheln (Tag-Strom & Wärmepumpe)
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            // Kachel: Tag-Strom
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .background(
                                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                                                        shape = RoundedCornerShape(8.dp)
                                                                    )
                                                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                                            ) {
                                                                Column {
                                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                                        Icon(
                                                                            Icons.Default.Bolt,
                                                                            contentDescription = null,
                                                                            tint = MaterialTheme.colorScheme.primary,
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Text(
                                                                            "Tag-Strom",
                                                                            style = MaterialTheme.typography.labelMedium,
                                                                            color = MaterialTheme.colorScheme.primary,
                                                                            fontWeight = FontWeight.SemiBold
                                                                        )
                                                                    }
                                                                    Spacer(modifier = Modifier.height(4.dp))
                                                                    Text(
                                                                        formatKwhValue(reading.normalReading),
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                }
                                                            }

                                                            // Kachel: Wärmepumpe
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .background(
                                                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                                                        shape = RoundedCornerShape(8.dp)
                                                                    )
                                                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                                            ) {
                                                                Column {
                                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                                        Icon(
                                                                            Icons.Default.ElectricMeter,
                                                                            contentDescription = null,
                                                                            tint = MaterialTheme.colorScheme.secondary,
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Text(
                                                                            "Wärmepumpe",
                                                                            style = MaterialTheme.typography.labelMedium,
                                                                            color = MaterialTheme.colorScheme.secondary,
                                                                            fontWeight = FontWeight.SemiBold
                                                                        )
                                                                    }
                                                                    Spacer(modifier = Modifier.height(4.dp))
                                                                    Text(
                                                                        formatKwhValue(reading.heatPumpReading),
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        // Notiz (falls vorhanden)
                                                        if (reading.note.isNotBlank()) {
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                            Text(
                                                                "Notiz: ${reading.note}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.padding(horizontal = 4.dp)
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.height(4.dp))

                                                        // Symbole darunter
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                "Doppelklick zum Bearbeiten",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                                modifier = Modifier.padding(start = 4.dp)
                                                            )
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                IconButton(
                                                                    onClick = { openEditDialog(reading) },
                                                                    modifier = Modifier.size(36.dp)
                                                                ) {
                                                                    Icon(
                                                                        Icons.Default.Edit,
                                                                        contentDescription = "Zählerstand bearbeiten",
                                                                        tint = MaterialTheme.colorScheme.primary,
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }
                                                                IconButton(
                                                                    onClick = {
                                                                        readingToAssign = reading
                                                                        showAssignDialog = true
                                                                    },
                                                                    modifier = Modifier.size(36.dp)
                                                                ) {
                                                                    Icon(
                                                                        Icons.Default.Person,
                                                                        contentDescription = "Mieter zuordnen",
                                                                        tint = MaterialTheme.colorScheme.secondary,
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }
                                                                IconButton(
                                                                    onClick = { onDeleteReading(reading.id) },
                                                                    modifier = Modifier
                                                                        .size(36.dp)
                                                                        .testTag("delete_reading_${reading.id}")
                                                                ) {
                                                                    Icon(
                                                                        Icons.Default.Delete,
                                                                        contentDescription = "Löschen",
                                                                        tint = MaterialTheme.colorScheme.error,
                                                                        modifier = Modifier.size(18.dp)
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
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 2: Berechnete Intervalle & Kosten (Collapsible with Month Sub-folders)
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { intervalsSectionExpanded = !intervalsSectionExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Berechnete Intervalle\n& Kosten (${intervals.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { intervalsSectionExpanded = !intervalsSectionExpanded }) {
                                Icon(
                                    if (intervalsSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Ein-/Ausklappen"
                                )
                            }
                        }

                        if (intervalsSectionExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            if (intervals.isEmpty()) {
                                Text(
                                    "Keine Intervalle berechnet (mind. 2 Zählerstände erforderlich).",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            } else {
                                val intervalsByMonth = intervals.groupBy { report ->
                                    extractMonthKey(report.endString)
                                }.toSortedMap(reverseOrder())

                                val currentCalendarMonth = SimpleDateFormat("yyyy-MM", Locale.GERMAN).format(java.util.Date())
                                val activeIntervalMonthKey = if (intervalsByMonth.containsKey(currentCalendarMonth)) {
                                    currentCalendarMonth
                                } else {
                                    intervalsByMonth.firstKey()
                                }

                                intervalsByMonth.forEach { (yearMonth, monthIntervals) ->
                                    val monthTitle = formatMonthYear(yearMonth)
                                    val defaultIntervalExpanded = if (intervalsByMonth.size > 1) {
                                        yearMonth == activeIntervalMonthKey
                                    } else {
                                        true
                                    }
                                    val isIntervalMonthExpanded = expandedIntervalMonths[yearMonth] ?: defaultIntervalExpanded

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        // Month Folder Header
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .clickable {
                                                    expandedIntervalMonths[yearMonth] = !isIntervalMonthExpanded
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "$monthTitle (${monthIntervals.size})",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            Icon(
                                                if (isIntervalMonthExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Month Interval Items
                                        if (isIntervalMonthExpanded) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            monthIntervals.forEach { report ->
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(12.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            val repStart = com.example.utils.DateUtils.formatToGermanDate(report.startString)
                                                            val repEnd = com.example.utils.DateUtils.formatToGermanDate(report.endString)
                                                            Text("$repStart → $repEnd", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                            Text("${report.days} Tage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                        Spacer(modifier = Modifier.height(6.dp))

                                                        // Tag-Strom und Wärmepumpe in Blockausrichtung
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
                                                            Text("Gesamt: ${report.totalConsumption} kWh", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                            Text("Kosten: ${String.format(Locale.GERMAN, "%.2f €", report.totalCost)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                                        }
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Button(
                                                            onClick = { onExportPdf(report) },
                                                            modifier = Modifier.align(Alignment.End),
                                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                                        ) {
                                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("PDF Exportieren")
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
                }
            }
        }
    }
}
