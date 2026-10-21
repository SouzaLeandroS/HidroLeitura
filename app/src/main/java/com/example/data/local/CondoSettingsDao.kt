package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CondoSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface CondoSettingsDao {
    @Query("SELECT * FROM condo_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<CondoSettings?>

    @Query("SELECT * FROM condo_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOnce(): CondoSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: CondoSettings)
}
