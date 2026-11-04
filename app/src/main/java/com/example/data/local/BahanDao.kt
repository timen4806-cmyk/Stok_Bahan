package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BahanDao {
    @Query("SELECT * FROM bahan_items WHERE userId = :userId ORDER BY id ASC")
    fun getBahanForUser(userId: String): Flow<List<BahanEntity>>

    @Query("SELECT COUNT(*) FROM bahan_items WHERE userId = :userId")
    suspend fun getCountForUser(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BahanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BahanEntity>)

    @Update
    suspend fun update(item: BahanEntity)

    @Query("UPDATE bahan_items SET nama = :nama, updatedAt = :timestamp WHERE id = :id AND userId = :userId")
    suspend fun updateName(id: Long, userId: String, nama: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE bahan_items SET nilai1 = :value, updatedAt = :timestamp WHERE id = :id AND userId = :userId")
    suspend fun updateNilai1(id: Long, userId: String, value: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE bahan_items SET nilai2 = :value, updatedAt = :timestamp WHERE id = :id AND userId = :userId")
    suspend fun updateNilai2(id: Long, userId: String, value: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE bahan_items SET nilai3 = :value, updatedAt = :timestamp WHERE id = :id AND userId = :userId")
    suspend fun updateNilai3(id: Long, userId: String, value: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE bahan_items SET nilai4 = :value, updatedAt = :timestamp WHERE id = :id AND userId = :userId")
    suspend fun updateNilai4(id: Long, userId: String, value: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE bahan_items SET nilai5 = :value, updatedAt = :timestamp WHERE id = :id AND userId = :userId")
    suspend fun updateNilai5(id: Long, userId: String, value: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM bahan_items WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: Long, userId: String)

    @Query("DELETE FROM bahan_items WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
