package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.BahanDefaults
import com.example.data.local.AppDatabase
import com.example.data.local.BahanEntity
import com.example.data.local.PengaturanEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.BahanItem
import com.example.data.model.Pengaturan
import com.example.data.model.UserProfile
import com.example.util.FirestoreErrorHelper
import com.example.util.OperationType
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StokRepository(
    val appDatabase: AppDatabase?,
    val firestore: FirebaseFirestore? = null
) {

    constructor(context: Context) : this(
        appDatabase = AppDatabase.getInstance(context),
        firestore = try {
            FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            )
        } catch (_: Exception) {
            null
        }
    )

    constructor(firestore: FirebaseFirestore) : this(
        appDatabase = null,
        firestore = firestore
    )

    private fun isFirebaseAuthActive(): Boolean {
        return try {
            Firebase.auth.currentUser != null
        } catch (_: Throwable) {
            false
        }
    }

    fun observeBahan(userId: String): Flow<List<BahanItem>> {
        require(userId.isNotBlank()) { "User ID must not be blank" }

        if (appDatabase != null) {
            return appDatabase.bahanDao().getBahanForUser(userId).map { list ->
                list.map { it.toBahanItem() }
            }
        }

        // Fallback for tests targeting Firestore emulator
        return flow {
            val db = firestore ?: error("No database available")
            val path = "users/$userId/bahan"
            val query = db.collection("users").document(userId).collection("bahan")
            emitAll(
                query.snapshots()
                    .map { snapshot -> snapshot.toObjects(BahanItem::class.java) }
                    .catch { error ->
                        if (error is Exception) {
                            FirestoreErrorHelper.handleFirestoreError(error, OperationType.LIST, path)
                        }
                        throw error
                    }
            )
        }
    }

    fun observeSettings(userId: String): Flow<Pengaturan> {
        require(userId.isNotBlank()) { "User ID must not be blank" }

        if (appDatabase != null) {
            return appDatabase.pengaturanDao().getPengaturanForUser(userId).map {
                it?.toPengaturan() ?: Pengaturan()
            }
        }

        return flow {
            val db = firestore ?: error("No database available")
            val path = "users/$userId/settings/config"
            val docRef = db.collection("users").document(userId).collection("settings").document("config")
            emitAll(
                docRef.snapshots()
                    .map { snapshot ->
                        snapshot.toObject(Pengaturan::class.java) ?: Pengaturan()
                    }
                    .catch { error ->
                        if (error is Exception) {
                            FirestoreErrorHelper.handleFirestoreError(error, OperationType.READ, path)
                        }
                        throw error
                    }
            )
        }
    }

    fun observeProfile(userId: String): Flow<UserProfile> {
        require(userId.isNotBlank()) { "User ID must not be blank" }

        if (appDatabase != null) {
            return appDatabase.userProfileDao().getProfileForUser(userId).map {
                it?.toUserProfile() ?: UserProfile()
            }
        }

        return flow {
            val db = firestore ?: error("No database available")
            val path = "users/$userId/profile/info"
            val docRef = db.collection("users").document(userId).collection("profile").document("info")
            emitAll(
                docRef.snapshots()
                    .map { snapshot ->
                        snapshot.toObject(UserProfile::class.java) ?: UserProfile()
                    }
                    .catch { error ->
                        if (error is Exception) {
                            FirestoreErrorHelper.handleFirestoreError(error, OperationType.READ, path)
                        }
                        throw error
                    }
            )
        }
    }

    suspend fun addBahan(
        userId: String,
        nama: String,
        n1: String = "",
        n2: String = "",
        n3: String = "",
        n4: String = "",
        n5: String = ""
    ): Result<String> {
        return try {
            require(userId.isNotBlank()) { "User ID must not be blank" }
            require(nama.isNotBlank()) { "Nama bahan tidak boleh kosong" }

            if (appDatabase != null) {
                val entity = BahanEntity(
                    userId = userId,
                    nama = nama.trim(),
                    nilai1 = n1.trim(),
                    nilai2 = n2.trim(),
                    nilai3 = n3.trim(),
                    nilai4 = n4.trim(),
                    nilai5 = n5.trim()
                )
                val newId = appDatabase.bahanDao().insert(entity)

                // If Firebase auth is active, sync to cloud
                if (isFirebaseAuthActive() && firestore != null) {
                    try {
                        val colRef = firestore.collection("users").document(userId).collection("bahan")
                        val payload = mapOf(
                            "nama" to nama.trim(),
                            "nilai1" to n1.trim(),
                            "nilai2" to n2.trim(),
                            "nilai3" to n3.trim(),
                            "nilai4" to n4.trim(),
                            "nilai5" to n5.trim(),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        colRef.add(payload).await()
                    } catch (_: Exception) {}
                }

                return Result.success(newId.toString())
            }

            // Direct Firestore for test suites
            val db = firestore ?: error("No database available")
            val colRef = db.collection("users").document(userId).collection("bahan")
            val payload = mapOf(
                "nama" to nama.trim(),
                "nilai1" to n1.trim(),
                "nilai2" to n2.trim(),
                "nilai3" to n3.trim(),
                "nilai4" to n4.trim(),
                "nilai5" to n5.trim(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val docRef = colRef.add(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHelper.handleFirestoreError(e, OperationType.CREATE, "users/$userId/bahan")
            Result.failure(e)
        }
    }

    suspend fun updateBahan(userId: String, item: BahanItem): Result<Unit> {
        return try {
            require(userId.isNotBlank()) { "User ID must not be blank" }
            require(item.id.isNotBlank()) { "Item ID must not be blank" }
            require(item.nama.isNotBlank()) { "Nama bahan tidak boleh kosong" }

            if (appDatabase != null) {
                val longId = item.id.toLongOrNull() ?: 0L
                appDatabase.bahanDao().updateName(longId, userId, item.nama.trim())
                return Result.success(Unit)
            }

            val db = firestore ?: error("No database available")
            val docRef = db.collection("users").document(userId).collection("bahan").document(item.id)
            val updates = mapOf(
                "nama" to item.nama.trim(),
                "nilai1" to item.nilai1.trim(),
                "nilai2" to item.nilai2.trim(),
                "nilai3" to item.nilai3.trim(),
                "nilai4" to item.nilai4.trim(),
                "nilai5" to item.nilai5.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHelper.handleFirestoreError(e, OperationType.UPDATE, "users/$userId/bahan/${item.id}")
            Result.failure(e)
        }
    }

    suspend fun updateBahanCell(userId: String, itemId: String, field: String, value: String): Result<Unit> {
        return try {
            require(userId.isNotBlank()) { "User ID must not be blank" }
            require(itemId.isNotBlank()) { "Item ID must not be blank" }

            if (appDatabase != null) {
                val longId = itemId.toLongOrNull() ?: 0L
                when (field) {
                    "nilai1" -> appDatabase.bahanDao().updateNilai1(longId, userId, value)
                    "nilai2" -> appDatabase.bahanDao().updateNilai2(longId, userId, value)
                    "nilai3" -> appDatabase.bahanDao().updateNilai3(longId, userId, value)
                    "nilai4" -> appDatabase.bahanDao().updateNilai4(longId, userId, value)
                    "nilai5" -> appDatabase.bahanDao().updateNilai5(longId, userId, value)
                }
                return Result.success(Unit)
            }

            val db = firestore ?: error("No database available")
            val docRef = db.collection("users").document(userId).collection("bahan").document(itemId)
            val updates = mapOf(
                field to value.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHelper.handleFirestoreError(e, OperationType.UPDATE, "users/$userId/bahan/$itemId")
            Result.failure(e)
        }
    }

    suspend fun deleteBahan(userId: String, itemId: String): Result<Unit> {
        return try {
            require(userId.isNotBlank()) { "User ID must not be blank" }
            require(itemId.isNotBlank()) { "Item ID must not be blank" }

            if (appDatabase != null) {
                val longId = itemId.toLongOrNull() ?: 0L
                appDatabase.bahanDao().deleteById(longId, userId)
                return Result.success(Unit)
            }

            val db = firestore ?: error("No database available")
            val docRef = db.collection("users").document(userId).collection("bahan").document(itemId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHelper.handleFirestoreError(e, OperationType.DELETE, "users/$userId/bahan/$itemId")
            Result.failure(e)
        }
    }

    suspend fun updateSettings(userId: String, settings: Pengaturan): Result<Unit> {
        return try {
            require(userId.isNotBlank()) { "User ID must not be blank" }

            if (appDatabase != null) {
                val entity = PengaturanEntity(
                    userId = userId,
                    judul = settings.judul.trim(),
                    kolom1 = settings.kolom1.trim(),
                    kolom2 = settings.kolom2.trim(),
                    kolom3 = settings.kolom3.trim(),
                    kolom4 = settings.kolom4.trim(),
                    kolom5 = settings.kolom5.trim()
                )
                appDatabase.pengaturanDao().insertOrUpdate(entity)
                return Result.success(Unit)
            }

            val db = firestore ?: error("No database available")
            val docRef = db.collection("users").document(userId).collection("settings").document("config")
            val payload = mapOf(
                "judul" to settings.judul.trim(),
                "kolom1" to settings.kolom1.trim(),
                "kolom2" to settings.kolom2.trim(),
                "kolom3" to settings.kolom3.trim(),
                "kolom4" to settings.kolom4.trim(),
                "kolom5" to settings.kolom5.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHelper.handleFirestoreError(e, OperationType.UPDATE, "users/$userId/settings/config")
            Result.failure(e)
        }
    }

    suspend fun updateProfile(userId: String, profile: UserProfile): Result<Unit> {
        return try {
            require(userId.isNotBlank()) { "User ID must not be blank" }

            if (appDatabase != null) {
                val entity = UserProfileEntity(
                    userId = userId,
                    email = profile.email.trim(),
                    displayName = profile.displayName.trim(),
                    twoFactorEnabled = profile.twoFactorEnabled,
                    twoFactorPin = profile.twoFactorPin,
                    autoBackupCloud = profile.autoBackupCloud,
                    themeMode = profile.themeMode
                )
                appDatabase.userProfileDao().insertOrUpdate(entity)
                return Result.success(Unit)
            }

            val db = firestore ?: error("No database available")
            val docRef = db.collection("users").document(userId).collection("profile").document("info")
            val payload = mapOf(
                "email" to profile.email.trim(),
                "displayName" to profile.displayName.trim(),
                "twoFactorEnabled" to profile.twoFactorEnabled,
                "twoFactorPin" to profile.twoFactorPin,
                "autoBackupCloud" to profile.autoBackupCloud,
                "themeMode" to profile.themeMode,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHelper.handleFirestoreError(e, OperationType.UPDATE, "users/$userId/profile/info")
            Result.failure(e)
        }
    }

    suspend fun seedInitialDataIfEmpty(userId: String): Result<Unit> {
        return try {
            require(userId.isNotBlank()) { "User ID must not be blank" }

            if (appDatabase != null) {
                val count = appDatabase.bahanDao().getCountForUser(userId)
                if (count == 0) {
                    val entities = BahanDefaults.BAHAN_AWAL.map { nama ->
                        BahanEntity(
                            userId = userId,
                            nama = nama,
                            nilai1 = "",
                            nilai2 = "",
                            nilai3 = "",
                            nilai4 = "",
                            nilai5 = ""
                        )
                    }
                    appDatabase.bahanDao().insertAll(entities)

                    appDatabase.pengaturanDao().insertOrUpdate(
                        PengaturanEntity(
                            userId = userId,
                            judul = "STOK BAHAN SINAR MAS",
                            kolom1 = "KOLOM 1",
                            kolom2 = "KOLOM 2",
                            kolom3 = "KOLOM 3",
                            kolom4 = "KOLOM 4",
                            kolom5 = "KOLOM 5"
                        )
                    )
                }
                return Result.success(Unit)
            }

            val db = firestore ?: error("No database available")
            val colRef = db.collection("users").document(userId).collection("bahan")
            val snapshot = colRef.limit(1).get().await()
            if (snapshot.isEmpty) {
                val batch = db.batch()
                for (nama in BahanDefaults.BAHAN_AWAL) {
                    val docRef = colRef.document()
                    val payload = mapOf(
                        "nama" to nama,
                        "nilai1" to "",
                        "nilai2" to "",
                        "nilai3" to "",
                        "nilai4" to "",
                        "nilai5" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(docRef, payload)
                }
                batch.commit().await()

                val settingsRef = db.collection("users").document(userId).collection("settings").document("config")
                settingsRef.set(
                    mapOf(
                        "judul" to "STOK BAHAN SINAR MAS",
                        "kolom1" to "KOLOM 1",
                        "kolom2" to "KOLOM 2",
                        "kolom3" to "KOLOM 3",
                        "kolom4" to "KOLOM 4",
                        "kolom5" to "KOLOM 5",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHelper.handleFirestoreError(e, OperationType.CREATE, "users/$userId/bahan")
            Result.failure(e)
        }
    }
}
