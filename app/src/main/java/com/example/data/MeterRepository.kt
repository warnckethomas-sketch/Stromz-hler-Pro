package com.example.data

import kotlinx.coroutines.flow.Flow

class MeterRepository(
    private val tenantDao: TenantDao,
    private val readingDao: MeterReadingDao,
    private val tariffDao: TariffDao,
    private val heatingSettingsDao: HeatingSettingsDao? = null
) {
    val allTenants: Flow<List<TenantEntity>> = tenantDao.getAllTenants()
    val tariffSettings: Flow<TariffSettingsEntity?> = tariffDao.getTariffSettings()
    val allHeatingSettings: Flow<List<HeatingSettingsEntity>>? = heatingSettingsDao?.getAllHeatingSettings()

    fun getReadingsForTenant(tenantId: Long): Flow<List<MeterReadingEntity>> {
        return readingDao.getReadingsForTenant(tenantId)
    }

    suspend fun insertTenant(tenant: TenantEntity): Long {
        return tenantDao.insertTenant(tenant)
    }

    suspend fun deleteTenant(id: Long) {
        tenantDao.deleteTenant(id)
    }

    suspend fun insertReading(reading: MeterReadingEntity) {
        readingDao.insertReading(reading)
    }

    suspend fun deleteReading(id: Long) {
        readingDao.deleteReading(id)
    }

    suspend fun updateTariffs(tariffs: TariffSettingsEntity) {
        tariffDao.insertTariff(tariffs)
    }

    suspend fun ensureDefaultTariffs() {
        if (tariffDao.getTariffSync() == null) {
            tariffDao.insertTariff(TariffSettingsEntity())
        }
    }

    suspend fun saveHeatingSettings(settings: HeatingSettingsEntity) {
        val dao = heatingSettingsDao ?: return
        val existing = dao.getByDateString(settings.effectiveDateString)
        val toSave = if (existing != null) settings.copy(id = existing.id) else settings
        dao.insertHeatingSettings(toSave)
    }

    suspend fun deleteHeatingSettings(id: Long) {
        heatingSettingsDao?.deleteHeatingSettings(id)
    }
}
