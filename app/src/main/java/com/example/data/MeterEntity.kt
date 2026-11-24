package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Entity(tableName = "heating_settings")
data class HeatingSettingsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val effectiveTimestamp: Long,
    val effectiveDateString: String, // format "yyyy-MM-dd"
    val egMode: String = "HEIZEN", // "HEIZEN" or "KUEHLEN"
    val egComfortTemp: Double = 21.0,
    val egEcoTemp: Double = 17.0,
    val egCoolTemp: Double = 24.0,
    val egScheduleType: String = "ALL", // "ALL" or "WEEKLY"
    val egAllDaysMask: String = "000000111000000011111100",
    val egWeeklyMasks: String = "",
    val ogHeatTemp: Double = 18.5,
    val ogSetbackTemp: Double = 14.0,
    val ogFrostTemp: Double = 5.0,
    val ogDayPrograms: String = "1,1,1,1,1,1,1",
    val updatedAt: Long = System.currentTimeMillis()
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

@Dao
interface HeatingSettingsDao {
    @Query("SELECT * FROM heating_settings ORDER BY effectiveDateString DESC, updatedAt DESC")
    fun getAllHeatingSettings(): Flow<List<HeatingSettingsEntity>>

    @Query("SELECT * FROM heating_settings ORDER BY effectiveDateString DESC, updatedAt DESC")
    suspend fun getAllHeatingSettingsList(): List<HeatingSettingsEntity>

    @Query("SELECT * FROM heating_settings WHERE effectiveDateString = :dateString LIMIT 1")
    suspend fun getByDateString(dateString: String): HeatingSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeatingSettings(settings: HeatingSettingsEntity): Long

    @Query("DELETE FROM heating_settings WHERE id = :id")
    suspend fun deleteHeatingSettings(id: Long)
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `heating_settings` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `effectiveTimestamp` INTEGER NOT NULL,
                `effectiveDateString` TEXT NOT NULL,
                `egMode` TEXT NOT NULL,
                `egComfortTemp` REAL NOT NULL,
                `egEcoTemp` REAL NOT NULL,
                `egCoolTemp` REAL NOT NULL,
                `egScheduleType` TEXT NOT NULL,
                `egAllDaysMask` TEXT NOT NULL,
                `egWeeklyMasks` TEXT NOT NULL,
                `ogHeatTemp` REAL NOT NULL,
                `ogSetbackTemp` REAL NOT NULL,
                `ogFrostTemp` REAL NOT NULL,
                `ogDayPrograms` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

@Database(
    entities = [
        TenantEntity::class,
        MeterReadingEntity::class,
        TariffSettingsEntity::class,
        PropertySettingsEntity::class,
        HeatingSettingsEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tenantDao(): TenantDao
    abstract fun meterReadingDao(): MeterReadingDao
    abstract fun tariffDao(): TariffDao
    abstract fun propertyDao(): PropertyDao
    abstract fun heatingSettingsDao(): HeatingSettingsDao
}
