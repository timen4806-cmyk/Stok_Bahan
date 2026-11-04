package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PengaturanDao {
    @Query("SELECT * FROM pengaturan_items WHERE userId = :userId LIMIT 1")
    fun getPengaturanForUser(userId: String): Flow<PengaturanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(pengaturan: PengaturanEntity)
}
