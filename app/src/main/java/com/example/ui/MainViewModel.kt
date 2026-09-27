package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.IntervalReport
import com.example.data.MeterReadingEntity
import com.example.data.MeterRepository
import com.example.data.PropertySettingsEntity
import com.example.data.TariffSettingsEntity
import com.example.data.TenantEntity
import com.example.data.calculateIntervals
import com.example.utils.BackupManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "strom_database"
    )
        .fallbackToDestructiveMigration()
        .build()

    private val repository = MeterRepository(db.tenantDao(), db.meterReadingDao(), db.tariffDao())

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp

    val tenants: StateFlow<List<TenantEntity>> = repository.allTenants
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedTenantId = MutableStateFlow<Long?>(null)
    val selectedTenantId: StateFlow<Long?> = _selectedTenantId

    val selectedTenant: StateFlow<TenantEntity?> = combine(tenants, _selectedTenantId) { tenantList, selectedId ->
        if (tenantList.isEmpty()) null
        else tenantList.find { it.id == selectedId } ?: tenantList.firstOrNull { it.isActive } ?: tenantList.first()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val readings: StateFlow<List<MeterReadingEntity>> = selectedTenant
        .flatMapLatest { tenant ->
            if (tenant == null) flowOf(emptyList())
            else repository.getReadingsForTenant(tenant.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val tariffs: StateFlow<TariffSettingsEntity> = repository.tariffSettings
        .combine(flowOf(TariffSettingsEntity())) { dbTariff, defaultTariff ->
            dbTariff ?: defaultTariff
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TariffSettingsEntity()
        )

    val propertySettings: StateFlow<PropertySettingsEntity> = db.propertyDao().getPropertySettings()
        .combine(flowOf(PropertySettingsEntity())) { dbProp, defaultProp ->
            dbProp ?: defaultProp
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PropertySettingsEntity()
        )

    val intervalReports: StateFlow<List<IntervalReport>> = readings
        .combine(tariffs) { readingList, tariffSettings ->
            calculateIntervals(readingList, tariffSettings)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.ensureDefaultTariffs()
            if (db.propertyDao().getPropertySync() == null) {
                db.propertyDao().insertProperty(PropertySettingsEntity())
            }
        }
        viewModelScope.launch {
            repository.allTenants.collect { list ->
                if (list.isEmpty()) {
                    val df = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
                    val today = df.format(Date())
                    val newId = repository.insertTenant(TenantEntity(name = "Mieter 1", startDate = today))
                    _selectedTenantId.value = newId
                } else if (_selectedTenantId.value == null || !list.any { it.id == _selectedTenantId.value }) {
                    val active = list.firstOrNull { it.isActive } ?: list.first()
                    _selectedTenantId.value = active.id
                }
            }
        }
    }

    fun triggerAutoBackup() {
        viewModelScope.launch {
            _isBackingUp.value = true
            delay(200)
            BackupManager.exportBackupToInternalStorage(getApplication(), db)
            delay(1200)
            _isBackingUp.value = false
        }
    }

    fun selectTenant(tenantId: Long) {
        _selectedTenantId.value = tenantId
    }

    fun addTenant(name: String, address: String, phone: String, startDate: String) {
        viewModelScope.launch {
            val newId = repository.insertTenant(TenantEntity(name = name, address = address, phone = phone, startDate = startDate, isActive = true))
            _selectedTenantId.value = newId
            triggerAutoBackup()
        }
    }

    fun updateTenantDetails(tenantId: Long, name: String, address: String, phone: String, startDate: String) {
        viewModelScope.launch {
            val tenantList = tenants.value
            val target = tenantList.find { it.id == tenantId } ?: return@launch
            val updated = target.copy(name = name, address = address, phone = phone, startDate = startDate)
            db.tenantDao().updateTenant(updated)
            triggerAutoBackup()
        }
    }

    fun archiveTenant(tenantId: Long, endDate: String) {
        viewModelScope.launch {
            val tenantList = tenants.value
            val target = tenantList.find { it.id == tenantId } ?: return@launch
            val updated = target.copy(isActive = false, endDate = endDate)
            db.tenantDao().updateTenant(updated)
            triggerAutoBackup()
        }
    }

    fun deleteTenant(tenantId: Long) {
        viewModelScope.launch {
            db.tenantDao().deleteTenant(tenantId)
            triggerAutoBackup()
        }
    }

    fun addReading(date: Long, dateString: String, normalReading: Double, heatPumpReading: Double, note: String) {
        val tenant = selectedTenant.value ?: return
        viewModelScope.launch {
            val entity = MeterReadingEntity(
                tenantId = tenant.id,
                date = date,
                dateString = dateString,
                normalReading = normalReading,
                heatPumpReading = heatPumpReading,
                note = note
            )
            repository.insertReading(entity)
            triggerAutoBackup()
        }
    }

    fun deleteReading(id: Long) {
        viewModelScope.launch {
            repository.deleteReading(id)
            triggerAutoBackup()
        }
    }

    fun updateReading(reading: MeterReadingEntity) {
        viewModelScope.launch {
            repository.insertReading(reading)
            triggerAutoBackup()
        }
    }

    fun updateReadingTenant(readingId: Long, newTenantId: Long) {
        viewModelScope.launch {
            db.meterReadingDao().updateReadingTenant(readingId, newTenantId)
            triggerAutoBackup()
        }
    }

    fun updateTariffs(normalWork: Double, normalBase: Double, hpWork: Double, hpBase: Double) {
        viewModelScope.launch {
            val updated = TariffSettingsEntity(
                id = 1,
                normalWorkPriceCt = normalWork,
                normalBaseFeeEurMonth = normalBase,
                heatPumpWorkPriceCt = hpWork,
                heatPumpBaseFeeEurMonth = hpBase
            )
            repository.updateTariffs(updated)
            triggerAutoBackup()
        }
    }

    fun updateProperty(name: String, address: String, meterNormal: String, meterHp: String) {
        viewModelScope.launch {
            val updated = PropertySettingsEntity(
                id = 1,
                propertyName = name,
                propertyAddress = address,
                meterNumberNormal = meterNormal,
                meterNumberHeatPump = meterHp
            )
            db.propertyDao().insertProperty(updated)
            triggerAutoBackup()
        }
    }

    suspend fun exportBackup(context: Context, uri: Uri): Boolean {
        return BackupManager.exportDatabaseToJson(context, db, uri)
    }

    suspend fun createAutoBackup(context: Context): Boolean {
        return BackupManager.exportBackupToInternalStorage(context, db)
    }

    suspend fun performExitBackup(context: Context): Boolean {
        _isBackingUp.value = true
        delay(300)
        val success = BackupManager.exportBackupToInternalStorage(context, db)
        delay(1500)
        _isBackingUp.value = false
        return success
    }

    suspend fun importBackup(context: Context, uri: Uri): Boolean {
        return BackupManager.importDatabaseFromJson(context, db, uri)
    }
}
