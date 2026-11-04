package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.UserProfile

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val email: String = "",
    val displayName: String = "",
    val twoFactorEnabled: Boolean = false,
    val twoFactorPin: String = "",
    val autoBackupCloud: Boolean = true,
    val themeMode: String = "SYSTEM",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toUserProfile(): UserProfile {
        return UserProfile(
            id = "info",
            email = email,
            displayName = displayName,
            twoFactorEnabled = twoFactorEnabled,
            twoFactorPin = twoFactorPin,
            autoBackupCloud = autoBackupCloud,
            themeMode = themeMode
        )
    }
}
