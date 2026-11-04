package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class BahanItem(
    @DocumentId val id: String = "",
    val nama: String = "",
    val nilai1: String = "",
    val nilai2: String = "",
    val nilai3: String = "",
    val nilai4: String = "",
    val nilai5: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Pengaturan(
    @DocumentId val id: String = "config",
    val judul: String = "STOK BAHAN SINAR MAS",
    val kolom1: String = "KOLOM 1",
    val kolom2: String = "KOLOM 2",
    val kolom3: String = "KOLOM 3",
    val kolom4: String = "KOLOM 4",
    val kolom5: String = "KOLOM 5",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "info",
    val email: String = "",
    val displayName: String = "",
    val twoFactorEnabled: Boolean = false,
    val twoFactorPin: String = "",
    val autoBackupCloud: Boolean = true,
    val themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}
