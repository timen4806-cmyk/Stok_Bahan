package com.example.util

import android.app.Activity
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.hardware.fingerprint.FingerprintManagerCompat

class BiometricAuthHelper(private val context: Context) {

    private var cancellationSignal: CancellationSignal? = null

    fun isBiometricHardwareAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val biometricManager = context.getSystemService(BiometricManager::class.java)
            biometricManager != null
        } else {
            val fingerprintManager = FingerprintManagerCompat.from(context)
            fingerprintManager.isHardwareDetected
        }
    }

    fun isBiometricEnrolled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val biometricManager = context.getSystemService(BiometricManager::class.java)
            biometricManager?.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.BIOMETRIC_STRONG
            ) == BiometricManager.BIOMETRIC_SUCCESS
        } else {
            val fingerprintManager = FingerprintManagerCompat.from(context)
            fingerprintManager.isHardwareDetected && fingerprintManager.hasEnrolledFingerprints()
        }
    }

    fun authenticate(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cancellationSignal = CancellationSignal()
            val executor = context.mainExecutor

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED
                    ) {
                        onError(errString?.toString() ?: "Otentikasi sidik jari dibatalkan")
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Sidik jari tidak dikenali. Silakan sentuh sensor kembali.")
                }
            }

            try {
                val prompt = BiometricPrompt.Builder(context)
                    .setTitle("Masuk dengan Sidik Jari")
                    .setSubtitle("Pindai sidik jari Anda")
                    .setDescription("Verifikasi biometrik untuk membuka akun Sinar Mas Aluminium")
                    .setNegativeButton("Gunakan Password", executor) { _, _ ->
                        onError("Beralih ke kata sandi biasa")
                    }
                    .build()

                prompt.authenticate(cancellationSignal!!, executor, callback)
            } catch (e: Exception) {
                onError("Gagal menginisialisasi sidik jari: ${e.localizedMessage}")
            }
        } else {
            onError("Sensor sidik jari memerlukan Android 9 ke atas")
        }
    }

    fun cancel() {
        cancellationSignal?.cancel()
        cancellationSignal = null
    }
}
