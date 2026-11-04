package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Pengaturan
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class StokRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addBahan_validPayload_createsDocumentAndReturnsId(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = StokRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addBahan(aliceUid, "PIPA 6mm", "10", "20", "30", "40", "50")
        }
        assertTrue(result.isSuccess)
        val docId = result.getOrThrow()
        assertTrue(docId.isNotEmpty())
    }

    @Test
    fun observeBahan_authenticatedOwner_emitsRealtimeItems(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = StokRepository(firestore)

        val docId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addBahan(aliceUid, "HOLO 10x14", "15").getOrThrow()
        }

        val emittedList = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeBahan(aliceUid).first { list -> list.any { it.id == docId } }
        }
        assertTrue(emittedList.any { it.id == docId })
    }

    @Test
    fun getBahan_crossUserAccess_failsWithPermissionDenied(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = StokRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addBahan(aliceUid, "PIPA 8mm", "100").getOrThrow()
        }

        // Bob tries to observe or read Alice's materials
        signInTestUser(BOB_EMAIL)
        val bobRepo = StokRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeBahan(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when Bob accesses Alice's materials")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    @Test
    fun observeBahan_unauthenticatedUser_failsWithPermissionDenied(): Unit = runBlocking {
        auth.signOut()
        val repository = StokRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeBahan("unauthenticated_uid").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated caller")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    @Test
    fun updateSettings_authenticatedOwner_succeeds(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = StokRepository(firestore)

        val settings = Pengaturan(
            judul = "GUDANG UTAMA SINAR MAS",
            kolom1 = "STOK TOKO",
            kolom2 = "STOK GUDANG"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateSettings(aliceUid, settings)
        }
        assertTrue(result.isSuccess)

        val saved = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSettings(aliceUid).first()
        }
        assertEquals("GUDANG UTAMA SINAR MAS", saved.judul)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@sinarmas.test"
        const val BOB_EMAIL = "bob@sinarmas.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
