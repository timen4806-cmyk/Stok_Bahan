package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.util.SecurityUtils

data class UserSession(
    val username: String,
    val displayName: String,
    val isLoggedIn: Boolean
)

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sinar_mas_session_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_USERNAME = "key_username"
        private const val KEY_DISPLAY_NAME = "key_display_name"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_USER_ACCOUNTS = "key_user_accounts"
    }

    init {
        // Initialize default administrator account if empty
        ensureDefaultAccount()
    }

    private fun ensureDefaultAccount() {
        val existing = prefs.getString(KEY_USER_ACCOUNTS, null)
        if (existing == null) {
            val defaultAccounts = mutableMapOf(
                "admin" to Pair("Admin Sinar Mas", SecurityUtils.hashPin("admin123")),
                "operator" to Pair("Operator Gudang", SecurityUtils.hashPin("123456"))
            )
            saveAccountsMap(defaultAccounts)
        }
    }

    private fun getAccountsMap(): MutableMap<String, Pair<String, String>> {
        val raw = prefs.getString(KEY_USER_ACCOUNTS, "") ?: ""
        val result = mutableMapOf<String, Pair<String, String>>()
        if (raw.isNotBlank()) {
            val entries = raw.split(";;")
            for (entry in entries) {
                val parts = entry.split("::")
                if (parts.size >= 3) {
                    val u = parts[0]
                    val d = parts[1]
                    val p = parts[2]
                    result[u] = Pair(d, p)
                }
            }
        }
        return result
    }

    private fun saveAccountsMap(map: Map<String, Pair<String, String>>) {
        val raw = map.entries.joinToString(";;") { (u, pair) ->
            "$u::${pair.first}::${pair.second}"
        }
        prefs.edit().putString(KEY_USER_ACCOUNTS, raw).apply()
    }

    fun registerUser(username: String, displayName: String, passwordRaw: String): Boolean {
        val map = getAccountsMap()
        val key = username.trim().lowercase()
        if (map.containsKey(key)) return false
        val hashed = SecurityUtils.hashPin(passwordRaw)
        map[key] = Pair(displayName.trim().ifBlank { username.trim() }, hashed)
        saveAccountsMap(map)
        return true
    }

    fun authenticateUser(username: String, passwordRaw: String): UserSession? {
        val map = getAccountsMap()
        val key = username.trim().lowercase()
        val account = map[key] ?: return null
        val (displayName, storedHash) = account
        return if (SecurityUtils.verifyPin(passwordRaw, storedHash)) {
            UserSession(username = key, displayName = displayName, isLoggedIn = true)
        } else {
            null
        }
    }

    fun authenticateLastBiometricUser(): UserSession {
        val currentUsername = prefs.getString(KEY_USERNAME, "admin") ?: "admin"
        val currentDisplayName = prefs.getString(KEY_DISPLAY_NAME, "Admin Sinar Mas") ?: "Admin Sinar Mas"
        return UserSession(username = currentUsername, displayName = currentDisplayName, isLoggedIn = true)
    }

    fun saveActiveSession(username: String, displayName: String) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USERNAME, username.lowercase())
            .putString(KEY_DISPLAY_NAME, displayName)
            .apply()
    }

    fun getActiveSession(): UserSession? {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return null
        val u = prefs.getString(KEY_USERNAME, "admin") ?: "admin"
        val d = prefs.getString(KEY_DISPLAY_NAME, "Admin Sinar Mas") ?: "Admin Sinar Mas"
        return UserSession(username = u, displayName = d, isLoggedIn = true)
    }

    fun clearSession() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }
}
