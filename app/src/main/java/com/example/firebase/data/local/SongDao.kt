package com.example.firebase.data.local


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(song: SongEntity)

    @Delete
    suspend fun deleteFavorite(song: SongEntity)

    // Flow nos permite observar los cambios en la base de datos en tiempo real
    @Query("SELECT * FROM favorite_songs")
    fun getAllFavorites(): Flow<List<SongEntity>>
}