package com.example.data

import kotlinx.coroutines.flow.Flow

class MeterRepository(
    private val tenantDao: TenantDao,
    private val readingDao: MeterReadingDao,
    private val tariffDao: TariffDao
) {
    val allTenants: Flow<List<TenantEntity>> = tenantDao.getAllTenants()
    val tariffSettings: Flow<TariffSettingsEntity?> = tariffDao.getTariffSettings()

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
}
