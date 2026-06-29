package com.example.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.TravelLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelDao {
    @Query("SELECT * FROM locations")
    fun getAllLocations(): Flow<List<TravelLocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<TravelLocationEntity>)
    
    @Query("DELETE FROM locations")
    suspend fun clearAll()
}
