package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Apartment
import kotlinx.coroutines.flow.Flow

@Dao
interface ApartmentDao {
    @Query("SELECT * FROM apartments ORDER BY block ASC, unitNumber ASC")
    fun getAllApartments(): Flow<List<Apartment>>

    @Query("SELECT * FROM apartments WHERE id = :id LIMIT 1")
    suspend fun getApartmentById(id: Long): Apartment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApartment(apartment: Apartment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApartments(apartments: List<Apartment>)

    @Update
    suspend fun updateApartment(apartment: Apartment)

    @Delete
    suspend fun deleteApartment(apartment: Apartment)

    @Query("SELECT COUNT(*) FROM apartments")
    suspend fun countApartments(): Int
}
