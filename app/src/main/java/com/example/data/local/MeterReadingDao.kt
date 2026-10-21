package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MeterReading
import kotlinx.coroutines.flow.Flow

@Dao
interface MeterReadingDao {
    @Query("SELECT * FROM meter_readings ORDER BY readingDate DESC")
    fun getAllReadings(): Flow<List<MeterReading>>

    @Query("SELECT * FROM meter_readings WHERE apartmentId = :apartmentId ORDER BY referenceMonth DESC")
    fun getReadingsForApartment(apartmentId: Long): Flow<List<MeterReading>>

    @Query("SELECT * FROM meter_readings WHERE referenceMonth = :month ORDER BY readingDate DESC")
    fun getReadingsForMonth(month: String): Flow<List<MeterReading>>

    @Query("SELECT DISTINCT referenceMonth FROM meter_readings ORDER BY referenceMonth DESC")
    fun getAllRecordedMonths(): Flow<List<String>>

    @Query("SELECT * FROM meter_readings WHERE apartmentId = :apartmentId AND referenceMonth = :month LIMIT 1")
    suspend fun getReadingForApartmentAndMonth(apartmentId: Long, month: String): MeterReading?

    @Query("SELECT * FROM meter_readings WHERE apartmentId = :apartmentId AND referenceMonth < :month ORDER BY referenceMonth DESC LIMIT 1")
    suspend fun getPreviousReading(apartmentId: Long, month: String): MeterReading?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: MeterReading): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadings(readings: List<MeterReading>)

    @Update
    suspend fun updateReading(reading: MeterReading)

    @Delete
    suspend fun deleteReading(reading: MeterReading)

    @Query("DELETE FROM meter_readings WHERE apartmentId = :apartmentId AND referenceMonth = :month")
    suspend fun deleteReadingByApartmentAndMonth(apartmentId: Long, month: String)
}
