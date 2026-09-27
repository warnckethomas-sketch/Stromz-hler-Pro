package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tenants")
data class TenantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String = "",
    val phone: String = "",
    val startDate: String, // format "yyyy-MM-dd"
    val endDate: String = "", // empty if active
    val isActive: Boolean = true
)

@Entity(tableName = "meter_readings")
data class MeterReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long, // foreign key to TenantEntity
    val date: Long, // timestamp in ms
    val dateString: String, // format "yyyy-MM-dd"
    val normalReading: Double, // kWh (Zählerstand Normalstrom)
    val heatPumpReading: Double, // kWh (Zählerstand Wärmepumpe)
    val note: String = ""
)

@Entity(tableName = "tariff_settings")
data class TariffSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val normalWorkPriceCt: Double = 32.5, // ct/kWh
    val normalBaseFeeEurMonth: Double = 12.0, // €/month
    val heatPumpWorkPriceCt: Double = 24.0, // ct/kWh
    val heatPumpBaseFeeEurMonth: Double = 10.0 // €/month
)

@Entity(tableName = "property_settings")
data class PropertySettingsEntity(
    @PrimaryKey val id: Int = 1,
    val propertyName: String = "Mietobjekt",
    val propertyAddress: String = "",
    val meterNumberNormal: String = "",
    val meterNumberHeatPump: String = ""
)

@Dao
interface TenantDao {
    @Query("SELECT * FROM tenants ORDER BY id DESC")
    fun getAllTenants(): Flow<List<TenantEntity>>

    @Query("SELECT * FROM tenants ORDER BY id DESC")
    suspend fun getAllTenantsList(): List<TenantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenant(tenant: TenantEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTenant(tenant: TenantEntity)

    @Query("DELETE FROM tenants WHERE id = :id")
    suspend fun deleteTenant(id: Long)
}

@Dao
interface MeterReadingDao {
    @Query("SELECT * FROM meter_readings WHERE tenantId = :tenantId ORDER BY date ASC")
    fun getReadingsForTenant(tenantId: Long): Flow<List<MeterReadingEntity>>

    @Query("SELECT * FROM meter_readings ORDER BY date ASC")
    suspend fun getAllReadingsList(): List<MeterReadingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: MeterReadingEntity)

    @Query("DELETE FROM meter_readings WHERE id = :id")
    suspend fun deleteReading(id: Long)

    @Query("UPDATE meter_readings SET tenantId = :newTenantId WHERE id = :readingId")
    suspend fun updateReadingTenant(readingId: Long, newTenantId: Long)
}

@Dao
interface TariffDao {
    @Query("SELECT * FROM tariff_settings WHERE id = 1")
    fun getTariffSettings(): Flow<TariffSettingsEntity?>

    @Query("SELECT * FROM tariff_settings WHERE id = 1")
    suspend fun getTariffSync(): TariffSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTariff(tariff: TariffSettingsEntity)
}

@Dao
interface PropertyDao {
    @Query("SELECT * FROM property_settings WHERE id = 1")
    fun getPropertySettings(): Flow<PropertySettingsEntity?>

    @Query("SELECT * FROM property_settings WHERE id = 1")
    suspend fun getPropertySync(): PropertySettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperty(property: PropertySettingsEntity)
}

@Database(entities = [TenantEntity::class, MeterReadingEntity::class, TariffSettingsEntity::class, PropertySettingsEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tenantDao(): TenantDao
    abstract fun meterReadingDao(): MeterReadingDao
    abstract fun tariffDao(): TariffDao
    abstract fun propertyDao(): PropertyDao
}
