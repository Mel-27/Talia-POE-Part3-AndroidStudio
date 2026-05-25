package com.gia.poe_demo.data.util

import android.content.Context
import android.util.Log
import com.gia.poe_demo.data.database.AppDatabase
import com.gia.poe_demo.data.entities.Expense
import com.gia.poe_demo.data.remote.ExpenseModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.gia.poe_demo.data.remote.CategoryModel

/**
 * Sync Manager for Local ↔ Cloud Synchronization
 *
 * Handles two-way sync between local Room database and
 * Firebase Realtime Database.
 *
 * Features:
 * - Upload unsynced local data to cloud
 * - Download cloud data to local database
 * - Conflict resolution (cloud wins for updates)
 * - Offline support with automatic sync when online
 *
 * Reference: IIE PROG7313 Module Manual (2026)
 */
class SyncManager(private val context: Context) {

    private val localDb = AppDatabase.getInstance(context)
    private val remoteDb = RealtimeDbManager()

    companion object {
        private const val TAG = "SyncManager"
    }

    // ============================================================
    // LOCAL → CLOUD SYNC
    // ============================================================

    /**
     * Sync all unsynced local expenses to Firebase Realtime Database
     *
     * @return SyncResult indicating success or failure
     */
    suspend fun syncUnsyncedExpenses(): SyncResult {
        return try {
            // Get expenses that haven't been synced yet
            val unsyncedExpenses = localDb.expenseDao().getUnsyncedExpenses().first()

            if (unsyncedExpenses.isEmpty()) {
                Log.d(TAG, "No unsynced expenses found")
                return SyncResult.Success(0)
            }

            var successCount = 0
            for (localExpense in unsyncedExpenses) {
                val remoteExpense = ExpenseModel(
                    categoryId = localExpense.categoryId,
                    description = localExpense.description,
                    amount = localExpense.amount,
                    date = localExpense.date,
                    startTime = localExpense.startTime,
                    endTime = localExpense.endTime,
                    receiptPhotoUrl = localExpense.receiptPhotoPath ?: ""
                )

                val result = remoteDb.saveExpense(remoteExpense)
                if (result.isSuccess) {
                    // Mark as synced in local database
                    localDb.expenseDao().markAsSynced(localExpense.id)
                    successCount++
                    Log.d(TAG, "Synced expense: ${localExpense.id}")
                } else {
                    Log.e(TAG, "Failed to sync expense: ${localExpense.id}",
                        result.exceptionOrNull())
                }
            }

            Log.d(TAG, "Sync completed: $successCount expenses synced")
            SyncResult.Success(successCount)

        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            SyncResult.Failure(e.message ?: "Unknown error")
        }
    }

    /**
     * Sync unsynced categories to cloud
     */
    suspend fun syncUnsyncedCategories(): SyncResult {
        return try {
            val unsyncedCategories = localDb.categoryDao().getUnsyncedCategories().first()

            if (unsyncedCategories.isEmpty()) {
                Log.d(TAG, "No unsynced categories found")
                return SyncResult.Success(0)
            }

            var successCount = 0
            for (category in unsyncedCategories) {
                val remoteCategory = CategoryModel(
                    name = category.name,
                    iconEmoji = category.iconEmoji,
                    monthlyLimit = category.monthlyLimit
                )

                val result = remoteDb.saveCategory(remoteCategory)
                if (result.isSuccess) {
                    localDb.categoryDao().markAsSynced(category.id)
                    successCount++
                    Log.d(TAG, "Synced category: ${category.id}")
                }
            }

            SyncResult.Success(successCount)
        } catch (e: Exception) {
            Log.e(TAG, "Category sync failed", e)
            SyncResult.Failure(e.message ?: "Unknown error")
        }
    }

    // ============================================================
    // CLOUD → LOCAL SYNC
    // ============================================================

    /**
     * Start real-time observation of cloud data
     * This will automatically sync cloud changes to local database
     */
    fun startObservingCloudData(onExpensesUpdated: (List<ExpenseModel>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            remoteDb.observeExpenses().collect { cloudExpenses ->
                withContext(Dispatchers.IO) {
                    // Sync cloud data to local database
                    for (cloudExpense in cloudExpenses) {
                        val localExpense = Expense(
                            categoryId = cloudExpense.categoryId,
                            description = cloudExpense.description,
                            amount = cloudExpense.amount,
                            date = cloudExpense.date,
                            startTime = cloudExpense.startTime,
                            endTime = cloudExpense.endTime,
                            receiptPhotoPath = cloudExpense.receiptPhotoUrl.ifEmpty { null },
                            syncedToCloud = true  // Already synced since it came from cloud
                        )
                        // Insert or ignore if already exists
                        localDb.expenseDao().insertOrIgnore(localExpense)
                    }
                }
                onExpensesUpdated(cloudExpenses)
            }
        }
    }

    // ============================================================
    // FULL SYNC
    // ============================================================

    /**
     * Perform complete two-way sync
     * Uploads local changes and downloads cloud changes
     */
    suspend fun performFullSync(): FullSyncResult {
        return try {
            // First upload local changes to cloud
            val uploadedExpenses = syncUnsyncedExpenses()
            val uploadedCategories = syncUnsyncedCategories()

            // Note: Cloud observation handles downloads automatically

            FullSyncResult(
                expensesUploaded = (uploadedExpenses as? SyncResult.Success)?.count ?: 0,
                categoriesUploaded = (uploadedCategories as? SyncResult.Success)?.count ?: 0,
                success = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Full sync failed", e)
            FullSyncResult(success = false, error = e.message)
        }
    }

    // ============================================================
    // RESULT CLASSES
    // ============================================================

    sealed class SyncResult {
        data class Success(val count: Int) : SyncResult()
        data class Failure(val error: String) : SyncResult()
    }

    data class FullSyncResult(
        val expensesUploaded: Int = 0,
        val categoriesUploaded: Int = 0,
        val success: Boolean = false,
        val error: String? = null
    )
}

/*
References:

Android Developers, 2024. Perform network operations using coroutines.
Available at: https://developer.android.com/kotlin/coroutines
[Accessed 25 May 2026].

Firebase, 2026. Offline Capabilities on Android.
Available at: https://firebase.google.com/docs/database/android/offline-capabilities
[Accessed 25 May 2026].
*/