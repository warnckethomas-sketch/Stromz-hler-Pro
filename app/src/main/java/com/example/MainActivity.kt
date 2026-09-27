package com.example

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainViewModel
import com.example.ui.screens.AddReadingScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.TariffSetupScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.PdfExporter
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val scope = rememberCoroutineScope()
                    val tenants by viewModel.tenants.collectAsState()
                    val selectedTenant by viewModel.selectedTenant.collectAsState()
                    val readings by viewModel.readings.collectAsState()
                    val tariffs by viewModel.tariffs.collectAsState()
                    val propertySettings by viewModel.propertySettings.collectAsState()
                    val intervals by viewModel.intervalReports.collectAsState()
                    val isBackingUp by viewModel.isBackingUp.collectAsState()

                    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        ActivityResultContracts.CreateDocument("application/json")
                    ) { uri ->
                        if (uri != null) {
                            scope.launch {
                                val success = viewModel.exportBackup(this@MainActivity, uri)
                                if (success) {
                                    Toast.makeText(this@MainActivity, "Backup erfolgreich gespeichert.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@MainActivity, "Fehler beim Speichern des Backups.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        ActivityResultContracts.GetContent()
                    ) { uri ->
                        if (uri != null) {
                            scope.launch {
                                val success = viewModel.importBackup(this@MainActivity, uri)
                                if (success) {
                                    Toast.makeText(this@MainActivity, "Daten erfolgreich wiederhergestellt.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@MainActivity, "Fehler beim Wiederherstellen.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                tenants = tenants,
                                selectedTenant = selectedTenant,
                                readings = readings,
                                intervals = intervals,
                                isBackingUp = isBackingUp,
                                onSelectTenant = { id -> viewModel.selectTenant(id) },
                                onUpdateReadingTenant = { readingId, newTenantId -> viewModel.updateReadingTenant(readingId, newTenantId) },
                                onNavigateToAdd = { navController.navigate("add_reading") },
                                onNavigateToTariffs = { navController.navigate("tariffs") },
                                onNavigateToAnalytics = { navController.navigate("analytics") },
                                onExportPdf = { report ->
                                    val pdfFile = PdfExporter.generateIntervalReportPdf(
                                        this@MainActivity,
                                        report,
                                        tariffs,
                                        propertySettings,
                                        selectedTenant
                                    )
                                    if (pdfFile != null && pdfFile.exists()) {
                                        val uri = FileProvider.getUriForFile(
                                            this@MainActivity,
                                            "${packageName}.fileprovider",
                                            pdfFile
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
                                        }
                                        try {
                                            startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(this@MainActivity, "PDF erstellt, aber kein PDF-Viewer gefunden.", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        Toast.makeText(this@MainActivity, "Fehler beim Erstellen des PDFs.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDeleteReading = { id ->
                                    viewModel.deleteReading(id)
                                },
                                onUpdateReading = { reading ->
                                    viewModel.updateReading(reading)
                                },
                                onPerformExitBackup = {
                                    viewModel.performExitBackup(this@MainActivity)
                                },
                                onExitApp = {
                                    try {
                                        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                                        am?.appTasks?.forEach { task ->
                                            task.finishAndRemoveTask()
                                        }
                                    } catch (_: Exception) {}
                                    finishAffinity()
                                    finishAndRemoveTask()
                                }
                            )
                        }
                        composable("add_reading") {
                            AddReadingScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onSaveReading = { date, dateString, normal, hp, note ->
                                    viewModel.addReading(date, dateString, normal, hp, note)
                                }
                            )
                        }
                        composable("tariffs") {
                            TariffSetupScreen(
                                tenants = tenants,
                                selectedTenant = selectedTenant,
                                currentTariffs = tariffs,
                                currentProperty = propertySettings,
                                onSelectTenant = { id -> viewModel.selectTenant(id) },
                                onAddTenant = { name, address, phone, start -> viewModel.addTenant(name, address, phone, start) },
                                onUpdateTenant = { id, name, address, phone, start -> viewModel.updateTenantDetails(id, name, address, phone, start) },
                                onArchiveTenant = { id, date -> viewModel.archiveTenant(id, date) },
                                onDeleteTenant = { id -> viewModel.deleteTenant(id) },
                                onExportBackup = {
                                    exportLauncher.launch("stromzaehler_backup.json")
                                },
                                onImportBackup = {
                                    importLauncher.launch("application/json")
                                },
                                onGoogleDriveBackup = {
                                    Toast.makeText(this@MainActivity, "Bitte wählen Sie im Speicherort-Dialog 'Google Drive' aus.", Toast.LENGTH_LONG).show()
                                    exportLauncher.launch("stromzaehler_gdrive_backup.json")
                                },
                                onNavigateBack = { navController.popBackStack() },
                                onSaveTariffs = { nWork, nBase, hWork, hBase ->
                                    viewModel.updateTariffs(nWork, nBase, hWork, hBase)
                                    Toast.makeText(this@MainActivity, "Tarife erfolgreich gespeichert", Toast.LENGTH_SHORT).show()
                                },
                                onSaveProperty = { name, address, meterNormal, meterHp ->
                                    viewModel.updateProperty(name, address, meterNormal, meterHp)
                                }
                            )
                        }
                        composable("analytics") {
                            AnalyticsScreen(
                                intervals = intervals,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
