package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.BahanItem
import com.example.data.model.Pengaturan
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StokRepositoryRoomTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: StokRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = StokRepository(appDatabase = database, firestore = null)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun seedInitialDataIfEmpty_seedsCatalogAndDefaultSettings(): Unit = runBlocking {
        val userId = "test_admin"
        val seedResult = repository.seedInitialDataIfEmpty(userId)
        assertTrue(seedResult.isSuccess)

        val materials = repository.observeBahan(userId).first()
        assertEquals(46, materials.size)
        assertTrue(materials.any { it.nama == "PIPA 6mm" })
        assertTrue(materials.any { it.nama == "HOLO KOTAK 14x14" })

        val settings = repository.observeSettings(userId).first()
        assertEquals("STOK BAHAN SINAR MAS", settings.judul)
        assertEquals("KOLOM 1", settings.kolom1)
    }

    @Test
    fun addAndObserveBahan_savesItemAccurately(): Unit = runBlocking {
        val userId = "test_user_1"
        val addResult = repository.addBahan(
            userId = userId,
            nama = "ALUMINIUM CUSTOM PROFILE",
            n1 = "100",
            n2 = "50",
            n3 = "20"
        )
        assertTrue(addResult.isSuccess)
        val newId = addResult.getOrThrow()

        val list = repository.observeBahan(userId).first()
        val item = list.firstOrNull { it.id == newId }
        assertNotNull(item)
        assertEquals("ALUMINIUM CUSTOM PROFILE", item?.nama)
        assertEquals("100", item?.nilai1)
        assertEquals("50", item?.nilai2)
    }

    @Test
    fun updateBahanCell_modifiesSpecifiedField(): Unit = runBlocking {
        val userId = "test_user_2"
        val newId = repository.addBahan(userId, "PIPA TEST", "10").getOrThrow()

        val updateResult = repository.updateBahanCell(userId, newId, "nilai3", "75")
        assertTrue(updateResult.isSuccess)

        val list = repository.observeBahan(userId).first()
        val item = list.first { it.id == newId }
        assertEquals("75", item.nilai3)
    }

    @Test
    fun deleteBahan_removesItem(): Unit = runBlocking {
        val userId = "test_user_3"
        val newId = repository.addBahan(userId, "TO BE DELETED", "5").getOrThrow()
        assertTrue(repository.observeBahan(userId).first().any { it.id == newId })

        val deleteResult = repository.deleteBahan(userId, newId)
        assertTrue(deleteResult.isSuccess)

        val list = repository.observeBahan(userId).first()
        assertTrue(list.none { it.id == newId })
    }

    @Test
    fun updateSettingsAndProfile_persistsCustomConfig(): Unit = runBlocking {
        val userId = "test_user_4"
        val settings = Pengaturan(
            judul = "GUDANG CABANG UTAMA",
            kolom1 = "READY",
            kolom2 = "BOOKING"
        )
        val setRes = repository.updateSettings(userId, settings)
        assertTrue(setRes.isSuccess)

        val observedSettings = repository.observeSettings(userId).first()
        assertEquals("GUDANG CABANG UTAMA", observedSettings.judul)
        assertEquals("READY", observedSettings.kolom1)

        val profile = UserProfile(
            displayName = "Kepala Gudang",
            twoFactorEnabled = true,
            themeMode = "DARK"
        )
        val profRes = repository.updateProfile(userId, profile)
        assertTrue(profRes.isSuccess)

        val observedProfile = repository.observeProfile(userId).first()
        assertEquals("Kepala Gudang", observedProfile.displayName)
        assertTrue(observedProfile.twoFactorEnabled)
        assertEquals("DARK", observedProfile.themeMode)
    }
}
