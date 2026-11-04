package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.SessionManager
import com.example.data.model.AppThemeMode
import com.example.data.repository.StokRepository
import com.example.ui.dialogs.TwoFactorDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.SinarMasTheme
import com.example.util.BiometricAuthHelper
import com.example.util.NotificationHelper
import com.example.viewmodel.AuthUiState
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.StokViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = StokRepository(this)
        val notificationHelper = NotificationHelper(this)
        val sessionManager = SessionManager(this)
        val biometricAuthHelper = BiometricAuthHelper(this)

        setContent {
            MainApp(
                repository = repository,
                notificationHelper = notificationHelper,
                sessionManager = sessionManager,
                biometricAuthHelper = biometricAuthHelper
            )
        }
    }
}

@Composable
fun MainApp(
    repository: StokRepository,
    notificationHelper: NotificationHelper,
    sessionManager: SessionManager,
    biometricAuthHelper: BiometricAuthHelper
) {
    val authViewModel = remember {
        AuthViewModel(sessionManager, repository, biometricAuthHelper)
    }

    var currentThemeMode by remember { mutableStateOf(AppThemeMode.SYSTEM) }
    val authUiState by authViewModel.uiState.collectAsState()

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            notificationHelper.showNotification(
                title = "Notifikasi Sinar Mas Aluminium Aktif",
                message = "Pembaruan inventaris dan stok bahan akan diberitahukan secara berkala."
            )
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationHelper.hasNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    SinarMasTheme(themeMode = currentThemeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (val state = authUiState) {
                is AuthUiState.Authenticated -> {
                    val session = state.session
                    val stokViewModel = remember(session.username) {
                        StokViewModel(repository, session.username, notificationHelper)
                    }

                    // Observe theme preference from profile
                    val profile by stokViewModel.profile.collectAsState()
                    LaunchedEffect(profile.themeMode) {
                        currentThemeMode = when (profile.themeMode) {
                            "LIGHT" -> AppThemeMode.LIGHT
                            "DARK" -> AppThemeMode.DARK
                            else -> AppThemeMode.SYSTEM
                        }
                    }

                    DashboardScreen(
                        userEmail = "${session.displayName} (@${session.username})",
                        stokViewModel = stokViewModel,
                        currentTheme = currentThemeMode,
                        onThemeChange = { mode ->
                            currentThemeMode = mode
                            stokViewModel.updateThemeMode(mode)
                        },
                        onSignOut = {
                            authViewModel.signOut()
                        }
                    )
                }

                is AuthUiState.TwoFactorRequired -> {
                    AuthScreen(
                        authViewModel = authViewModel,
                        uiState = state
                    )

                    TwoFactorDialog(
                        userEmail = state.session.displayName,
                        onVerify = { pin ->
                            authViewModel.verifyTwoFactorPin(
                                inputPin = pin,
                                storedHash = state.profile.twoFactorPin,
                                session = state.session
                            )
                        },
                        onDismiss = {
                            authViewModel.signOut()
                        }
                    )
                }

                else -> {
                    AuthScreen(
                        authViewModel = authViewModel,
                        uiState = state
                    )
                }
            }
        }
    }
}
