package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Pengaturan

@Entity(tableName = "pengaturan_items")
data class PengaturanEntity(
    @PrimaryKey val userId: String,
    val judul: String = "STOK BAHAN SINAR MAS",
    val kolom1: String = "KOLOM 1",
    val kolom2: String = "KOLOM 2",
    val kolom3: String = "KOLOM 3",
    val kolom4: String = "KOLOM 4",
    val kolom5: String = "KOLOM 5",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toPengaturan(): Pengaturan {
        return Pengaturan(
            id = "config",
            judul = judul,
            kolom1 = kolom1,
            kolom2 = kolom2,
            kolom3 = kolom3,
            kolom4 = kolom4,
            kolom5 = kolom5
        )
    }
}
