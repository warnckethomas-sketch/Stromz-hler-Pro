package com.example.utils

import android.content.Context
import android.net.Uri
import com.example.data.AppDatabase
import com.example.data.MeterReadingEntity
import com.example.data.TariffSettingsEntity
import com.example.data.TenantEntity
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class BackupData(
    val version: Int = 1,
    val tenants: List<TenantEntity>,
    val readings: List<MeterReadingEntity>,
    val tariffs: TariffSettingsEntity?
)

object BackupManager {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(BackupData::class.java)

    suspend fun exportDatabaseToJson(context: Context, db: AppDatabase, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val tenants = db.tenantDao().getAllTenantsList()
                val readings = db.meterReadingDao().getAllReadingsList()
                val tariffs = db.tariffDao().getTariffSync()

                val backup = BackupData(tenants = tenants, readings = readings, tariffs = tariffs)
                val jsonString = adapter.toJson(backup)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                    outputStream.flush()
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun exportBackupToInternalStorage(context: Context, db: AppDatabase): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val tenants = db.tenantDao().getAllTenantsList()
                val readings = db.meterReadingDao().getAllReadingsList()
                val tariffs = db.tariffDao().getTariffSync()

                val backup = BackupData(tenants = tenants, readings = readings, tariffs = tariffs)
                val jsonString = adapter.toJson(backup)

                val file = File(context.filesDir, "auto_backup_stromzaehler.json")
                file.writeText(jsonString)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun getBackupStatusInfo(context: Context): String {
        val file = File(context.filesDir, "auto_backup_stromzaehler.json")
        if (file.exists()) {
            val date = Date(file.lastModified())
            val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)
            return "Status: Gesichert (Interner Speicher, zuletzt: ${formatter.format(date)})"
        }
        return "Status: Noch kein automatisches Backup erstellt"
    }

    suspend fun importDatabaseFromJson(context: Context, db: AppDatabase, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val stringBuilder = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line)
                            line = reader.readLine()
                        }
                    }
                }

                val jsonStr = stringBuilder.toString()
                val backup = adapter.fromJson(jsonStr) ?: return@withContext false

                backup.tenants.forEach { db.tenantDao().insertTenant(it) }
                backup.readings.forEach { db.meterReadingDao().insertReading(it) }
                backup.tariffs?.let { db.tariffDao().insertTariff(it) }

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
