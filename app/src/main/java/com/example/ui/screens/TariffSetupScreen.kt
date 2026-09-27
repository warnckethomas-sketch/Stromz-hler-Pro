package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PropertySettingsEntity
import com.example.data.TariffSettingsEntity
import com.example.data.TenantEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TariffSetupScreen(
    tenants: List<TenantEntity>,
    selectedTenant: TenantEntity?,
    currentTariffs: TariffSettingsEntity,
    currentProperty: PropertySettingsEntity,
    onSelectTenant: (Long) -> Unit,
    onAddTenant: (String, String, String, String) -> Unit,
    onUpdateTenant: (Long, String, String, String, String) -> Unit,
    onArchiveTenant: (Long, String) -> Unit,
    onDeleteTenant: (Long) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onGoogleDriveBackup: () -> Unit,
    onNavigateBack: () -> Unit,
    onSaveTariffs: (Double, Double, Double, Double) -> Unit,
    onSaveProperty: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var normalWorkInput by remember { mutableStateOf(currentTariffs.normalWorkPriceCt.toString()) }
    var normalBaseInput by remember { mutableStateOf(currentTariffs.normalBaseFeeEurMonth.toString()) }
    var hpWorkInput by remember { mutableStateOf(currentTariffs.heatPumpWorkPriceCt.toString()) }
    var hpBaseInput by remember { mutableStateOf(currentTariffs.heatPumpBaseFeeEurMonth.toString()) }

    var propertyNameInput by remember { mutableStateOf(currentProperty.propertyName) }
    var propertyAddressInput by remember { mutableStateOf(currentProperty.propertyAddress) }
    var meterNormalInput by remember { mutableStateOf(currentProperty.meterNumberNormal) }
    var meterHpInput by remember { mutableStateOf(currentProperty.meterNumberHeatPump) }

    var showAddTenantDialog by remember { mutableStateOf(false) }
    var showEditTenantDialog by remember { mutableStateOf(false) }
    var tenantToEdit by remember { mutableStateOf<TenantEntity?>(null) }

    var showArchiveDialog by remember { mutableStateOf(false) }
    var tenantToArchive by remember { mutableStateOf<TenantEntity?>(null) }

    var newTenantName by remember { mutableStateOf("") }
    var newTenantAddress by remember { mutableStateOf("") }
    var newTenantPhone by remember { mutableStateOf("") }

    var editTenantName by remember { mutableStateOf("") }
    var editTenantAddress by remember { mutableStateOf("") }
    var editTenantPhone by remember { mutableStateOf("") }
    var editTenantStartDate by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
    var newTenantStartDate by remember { mutableStateOf(dateFormat.format(calendar.time)) }
    var archiveEndDate by remember { mutableStateOf(dateFormat.format(calendar.time)) }

    val startDatePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            newTenantStartDate = dateFormat.format(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val editStartDatePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            editTenantStartDate = dateFormat.format(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val archiveDatePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            archiveEndDate = dateFormat.format(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    if (showAddTenantDialog) {
        AlertDialog(
            onDismissRequest = { showAddTenantDialog = false },
            title = { Text("Neuen Mieter erfassen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Geben Sie Name, Anschrift, Telefon und Einzugsdatum ein:")
                    OutlinedTextField(
                        value = newTenantName,
                        onValueChange = { newTenantName = it },
                        label = { Text("Name des Mieters") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTenantAddress,
                        onValueChange = { newTenantAddress = it },
                        label = { Text("Anschrift (Straße, Hausnr., PLZ, Ort)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTenantPhone,
                        onValueChange = { newTenantPhone = it },
                        label = { Text("Telefonnummer") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTenantStartDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Einzugsdatum") },
                        trailingIcon = {
                            IconButton(onClick = { startDatePicker.show() }) {
                                Icon(Icons.Default.Person, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTenantName.isNotBlank()) {
                            onAddTenant(newTenantName.trim(), newTenantAddress.trim(), newTenantPhone.trim(), newTenantStartDate)
                            newTenantName = ""
                            newTenantAddress = ""
                            newTenantPhone = ""
                            showAddTenantDialog = false
                            scope.launch { snackbarHostState.showSnackbar("Neuer Mieter erfolgreich erfasst.") }
                        }
                    }
                ) {
                    Text("Hinzufügen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTenantDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (showEditTenantDialog && tenantToEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditTenantDialog = false },
            title = { Text("Mieter bearbeiten") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editTenantName,
                        onValueChange = { editTenantName = it },
                        label = { Text("Name des Mieters") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTenantAddress,
                        onValueChange = { editTenantAddress = it },
                        label = { Text("Anschrift") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTenantPhone,
                        onValueChange = { editTenantPhone = it },
                        label = { Text("Telefonnummer") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTenantStartDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Einzugsdatum") },
                        trailingIcon = {
                            IconButton(onClick = { editStartDatePicker.show() }) {
                                Icon(Icons.Default.Person, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val targetId = tenantToEdit!!.id
                            showEditTenantDialog = false
                            tenantToEdit = null
                            onDeleteTenant(targetId)
                            scope.launch { snackbarHostState.showSnackbar("Mieter gelöscht.") }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Löschen")
                    }

                    Button(
                        onClick = {
                            if (editTenantName.isNotBlank() && tenantToEdit != null) {
                                onUpdateTenant(tenantToEdit!!.id, editTenantName.trim(), editTenantAddress.trim(), editTenantPhone.trim(), editTenantStartDate)
                                showEditTenantDialog = false
                                tenantToEdit = null
                                scope.launch { snackbarHostState.showSnackbar("Mieter erfolgreich aktualisiert.") }
                            }
                        }
                    ) {
                        Text("Speichern")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditTenantDialog = false
                    tenantToEdit = null
                }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (showArchiveDialog && tenantToArchive != null) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text("Mieter archivieren (Auszug)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Möchten Sie den Mieter '${tenantToArchive!!.name}' archivieren? Tragen Sie das Auszugsdatum ein:")
                    OutlinedTextField(
                        value = archiveEndDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Auszugsdatum") },
                        trailingIcon = {
                            IconButton(onClick = { archiveDatePicker.show() }) {
                                Icon(Icons.Default.Archive, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onArchiveTenant(tenantToArchive!!.id, archiveEndDate)
                        showArchiveDialog = false
                        tenantToArchive = null
                        scope.launch { snackbarHostState.showSnackbar("Mieter erfolgreich archiviert.") }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Archivieren")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showArchiveDialog = false
                    tenantToArchive = null
                }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup & Konfiguration", fontWeight = FontWeight.Bold) },
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingVals ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ==========================================
            // SECTION 1: Mietobjekt & Anschrift
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mietobjekt & Zähler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = propertyNameInput,
                        onValueChange = { propertyNameInput = it },
                        label = { Text("Objektbezeichnung (z.B. Haus Nord)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = propertyAddressInput,
                        onValueChange = { propertyAddressInput = it },
                        label = { Text("Anschrift des Mietobjekts") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = meterNormalInput,
                        onValueChange = { meterNormalInput = it },
                        label = { Text("Zählernummer Tag-Strom (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = meterHpInput,
                        onValueChange = { meterHpInput = it },
                        label = { Text("Zählernummer Wärmepumpe (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onSaveProperty(propertyNameInput.trim(), propertyAddressInput.trim(), meterNormalInput.trim(), meterHpInput.trim())
                            scope.launch {
                                snackbarHostState.showSnackbar("Mietobjekt erfolgreich gespeichert.")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Mietobjekt speichern", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ==========================================
            // SECTION 2: Mieter-Verwaltung & Archiv
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mieter-Verwaltung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showAddTenantDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Neu")
                        }
                    }

                    if (tenants.isEmpty()) {
                        Text("Keine Mieter vorhanden.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        tenants.forEach { tenant ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (tenant.id == selectedTenant?.id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(tenant.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        if (tenant.id == selectedTenant?.id) {
                                            Text("Aktiv", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (tenant.address.isNotBlank()) {
                                        Text("Anschrift: ${tenant.address}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (tenant.phone.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Tel: ${tenant.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (tenant.isActive) {
                                        Text("Einzug: ${com.example.utils.DateUtils.formatToGermanDate(tenant.startDate)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        Text("Archiviert: ${com.example.utils.DateUtils.formatToGermanDate(tenant.startDate)} bis ${com.example.utils.DateUtils.formatToGermanDate(tenant.endDate)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (tenant.id != selectedTenant?.id) {
                                            TextButton(
                                                onClick = { onSelectTenant(tenant.id) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("Auswählen", fontSize = 12.sp)
                                            }
                                        }
                                        TextButton(
                                            onClick = {
                                                tenantToEdit = tenant
                                                editTenantName = tenant.name
                                                editTenantAddress = tenant.address
                                                editTenantPhone = tenant.phone
                                                editTenantStartDate = com.example.utils.DateUtils.formatToGermanDate(tenant.startDate)
                                                showEditTenantDialog = true
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Bearbeiten", fontSize = 12.sp)
                                        }
                                        if (tenant.isActive) {
                                            TextButton(
                                                onClick = {
                                                    tenantToArchive = tenant
                                                    showArchiveDialog = true
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Archivieren", fontSize = 12.sp)
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
            // SECTION 3: Stromtarife
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Stromtarife konfigurieren", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Text("Tag-Strom (Haushalt)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = normalWorkInput,
                        onValueChange = { normalWorkInput = it },
                        label = { Text("Arbeitspreis (ct/kWh)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("normal_work_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = normalBaseInput,
                        onValueChange = { normalBaseInput = it },
                        label = { Text("Grundgebühr (€/Monat)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("normal_base_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Wärmepumpe", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = hpWorkInput,
                        onValueChange = { hpWorkInput = it },
                        label = { Text("Arbeitspreis (ct/kWh)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hp_work_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = hpBaseInput,
                        onValueChange = { hpBaseInput = it },
                        label = { Text("Grundgebühr (€/Monat)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hp_base_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val nWork = normalWorkInput.replace(',', '.').toDoubleOrNull()
                            val nBase = normalBaseInput.replace(',', '.').toDoubleOrNull()
                            val hWork = hpWorkInput.replace(',', '.').toDoubleOrNull()
                            val hBase = hpBaseInput.replace(',', '.').toDoubleOrNull()

                            if (nWork == null || nBase == null || hWork == null || hBase == null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Bitte gültige Zahlenwerte eingeben.")
                                }
                                return@Button
                            }

                            onSaveTariffs(nWork, nBase, hWork, hBase)
                            scope.launch {
                                snackbarHostState.showSnackbar("Tarife erfolgreich gespeichert.")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_tariffs_button")
                    ) {
                        Text("Tarife speichern", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ==========================================
            // SECTION 4: Datenbank & Backup
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Datenbank & Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("Sichern Sie Ihre Daten oder stellen Sie diese von Google Drive oder dem Gerät wieder her.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onExportBackup,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sichern")
                        }
                        Button(
                            onClick = onImportBackup,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Laden")
                        }
                    }

                    Button(
                        onClick = onGoogleDriveBackup,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google Drive Synchronisation")
                    }
                }
            }
        }
    }
}
