package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.BahanItem

@Entity(tableName = "bahan_items")
data class BahanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val nama: String,
    val nilai1: String = "",
    val nilai2: String = "",
    val nilai3: String = "",
    val nilai4: String = "",
    val nilai5: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toBahanItem(): BahanItem {
        return BahanItem(
            id = id.toString(),
            nama = nama,
            nilai1 = nilai1,
            nilai2 = nilai2,
            nilai3 = nilai3,
            nilai4 = nilai4,
            nilai5 = nilai5
        )
    }
}
