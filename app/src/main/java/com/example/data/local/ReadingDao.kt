package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Reading
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {
    @Query("SELECT * FROM meter_readings ORDER BY readingDate DESC")
    fun getAllReadings(): Flow<List<Reading>>

    @Query("SELECT * FROM meter_readings WHERE apartmentId = :apartmentId ORDER BY referenceMonth DESC")
    fun getReadingsForApartment(apartmentId: Long): Flow<List<Reading>>

    @Query("SELECT * FROM meter_readings WHERE referenceMonth = :month ORDER BY readingDate DESC")
    fun getReadingsForMonth(month: String): Flow<List<Reading>>

    @Query("SELECT * FROM meter_readings WHERE apartmentId = :apartmentId AND referenceMonth = :month LIMIT 1")
    suspend fun getReadingForApartmentAndMonth(apartmentId: Long, month: String): Reading?

    @Query("SELECT * FROM meter_readings WHERE apartmentId = :apartmentId ORDER BY referenceMonth DESC LIMIT 1")
    suspend fun getLatestReadingForApartment(apartmentId: Long): Reading?

    @Query("SELECT * FROM meter_readings WHERE apartmentId = :apartmentId AND referenceMonth < :month ORDER BY referenceMonth DESC LIMIT 1")
    suspend fun getPreviousReadingForApartment(apartmentId: Long, month: String): Reading?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: Reading): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadings(readings: List<Reading>)

    @Update
    suspend fun updateReading(reading: Reading)

    @Delete
    suspend fun deleteReading(reading: Reading)

    @Query("DELETE FROM meter_readings WHERE apartmentId = :apartmentId")
    suspend fun deleteAllReadingsForApartment(apartmentId: Long)
}
