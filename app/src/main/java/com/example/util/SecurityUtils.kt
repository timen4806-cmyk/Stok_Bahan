package com.example.util

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {

    private const val AES_KEY_STRING = "SinarMasAluminiumSecretKey2026!#" // 32 chars for AES-256
    private const val GCM_TAG_LENGTH = 128
    private val FIXED_IV = byteArrayOf(12, 34, 56, 78, 90, 11, 22, 33, 44, 55, 66, 77) // 12-byte IV for GCM

    /**
     * Hashes a 2FA PIN using SHA-256 for secure storage and comparison.
     */
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.trim().toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    /**
     * Verifies if input PIN matches stored hash.
     */
    fun verifyPin(inputPin: String, storedHash: String): Boolean {
        if (storedHash.isBlank() || inputPin.isBlank()) return false
        val computed = hashPin(inputPin)
        return computed == storedHash
    }

    /**
     * End-to-end encryption for sensitive data text using AES/GCM/NoPadding.
     */
    fun encrypt(plainText: String): String {
        return try {
            val key = SecretKeySpec(AES_KEY_STRING.toByteArray(Charsets.UTF_8), "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, FIXED_IV)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(cipherText, Base64.NO_WRAP)
        } catch (e: Exception) {
            plainText
        }
    }

    /**
     * Decrypts encrypted text back to original.
     */
    fun decrypt(cipherText: String): String {
        return try {
            val key = SecretKeySpec(AES_KEY_STRING.toByteArray(Charsets.UTF_8), "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, FIXED_IV)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val decodedBytes = Base64.decode(cipherText, Base64.NO_WRAP)
            val plainBytes = cipher.doFinal(decodedBytes)
            String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            cipherText
        }
    }
}
