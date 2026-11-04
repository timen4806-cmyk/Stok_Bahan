package com.example.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SessionManager
import com.example.data.UserSession
import com.example.data.model.UserProfile
import com.example.data.repository.StokRepository
import com.example.util.BiometricAuthHelper
import com.example.util.SecurityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    object Unauthenticated : AuthUiState
    object Loading : AuthUiState
    data class TwoFactorRequired(val session: UserSession, val profile: UserProfile) : AuthUiState
    data class Authenticated(val session: UserSession) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
    private val sessionManager: SessionManager,
    private val repository: StokRepository,
    private val biometricAuthHelper: BiometricAuthHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Unauthenticated)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isBiometricSupported = MutableStateFlow(false)
    val isBiometricSupported: StateFlow<Boolean> = _isBiometricSupported.asStateFlow()

    init {
        _isBiometricSupported.value = biometricAuthHelper.isBiometricHardwareAvailable()
        checkActiveSession()
    }

    fun checkActiveSession() {
        val active = sessionManager.getActiveSession()
        if (active != null) {
            verifySessionSecurity(active)
        } else {
            _uiState.value = AuthUiState.Unauthenticated
        }
    }

    private fun verifySessionSecurity(session: UserSession) {
        viewModelScope.launch {
            try {
                val profile = repository.observeProfile(session.username).first()
                if (profile.twoFactorEnabled && profile.twoFactorPin.isNotBlank()) {
                    _uiState.value = AuthUiState.TwoFactorRequired(session, profile)
                } else {
                    _uiState.value = AuthUiState.Authenticated(session)
                    repository.seedInitialDataIfEmpty(session.username)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Authenticated(session)
                repository.seedInitialDataIfEmpty(session.username)
            }
        }
    }

    fun signInWithCredentials(username: String, passwordRaw: String) {
        if (username.isBlank()) {
            _uiState.value = AuthUiState.Error("Username atau email tidak boleh kosong")
            return
        }
        if (passwordRaw.isBlank()) {
            _uiState.value = AuthUiState.Error("Kata sandi tidak boleh kosong")
            return
        }

        _uiState.value = AuthUiState.Loading

        val session = sessionManager.authenticateUser(username, passwordRaw)
        if (session != null) {
            sessionManager.saveActiveSession(session.username, session.displayName)
            verifySessionSecurity(session)
        } else {
            _uiState.value = AuthUiState.Error("Username atau kata sandi tidak valid. Coba username 'admin' dan password 'admin123'")
        }
    }

    fun registerNewAccount(username: String, displayName: String, passwordRaw: String, confirmPasswordRaw: String) {
        if (username.isBlank()) {
            _uiState.value = AuthUiState.Error("Username tidak boleh kosong")
            return
        }
        if (passwordRaw.length < 4) {
            _uiState.value = AuthUiState.Error("Kata sandi minimal 4 karakter")
            return
        }
        if (passwordRaw != confirmPasswordRaw) {
            _uiState.value = AuthUiState.Error("Konfirmasi kata sandi tidak cocok")
            return
        }

        _uiState.value = AuthUiState.Loading

        val registered = sessionManager.registerUser(username, displayName, passwordRaw)
        if (registered) {
            signInWithCredentials(username, passwordRaw)
        } else {
            _uiState.value = AuthUiState.Error("Username sudah digunakan. Silakan pilih username lain.")
        }
    }

    fun signInWithBiometrics(activity: Activity) {
        _uiState.value = AuthUiState.Loading

        biometricAuthHelper.authenticate(
            activity = activity,
            onSuccess = {
                val session = sessionManager.authenticateLastBiometricUser()
                sessionManager.saveActiveSession(session.username, session.displayName)
                verifySessionSecurity(session)
            },
            onError = { err ->
                _uiState.value = AuthUiState.Error(err)
            }
        )
    }

    fun verifyTwoFactorPin(inputPin: String, storedHash: String, session: UserSession): Boolean {
        return if (SecurityUtils.verifyPin(inputPin, storedHash)) {
            _uiState.value = AuthUiState.Authenticated(session)
            true
        } else {
            false
        }
    }

    fun signOut() {
        sessionManager.clearSession()
        _uiState.value = AuthUiState.Unauthenticated
    }
}
