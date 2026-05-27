package com.gia.poe_demo.util

import android.content.Context
import android.util.Log
import com.gia.poe_demo.data.database.AppDatabase
import com.gia.poe_demo.data.entities.Expense
import com.gia.poe_demo.data.remote.ExpenseModel
import com.gia.poe_demo.data.util.RealtimeDbManager
import kotlinx.coroutines.flow.first

class SyncManager(private val context: Context) {

    private val localDb = AppDatabase.getInstance(context)
    private val remoteDb = RealtimeDbManager()

    companion object {
        private const val TAG = "SyncManager"
    }

    /**
     * Syncs all local expenses that have not yet been pushed to Firebase.
     * Marks each expense as synced in RoomDB on success.
     * Reference: Firebase Realtime Database Android Docs - https://firebase.google.com/docs/database/android/start
     * Reference: IIE PROG7313 Module Manual (2026)
     */
    suspend fun syncUnsyncedExpenses(): SyncResult {
        return try {
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
                    localDb.expenseDao().markAsSynced(localExpense.id)
                    successCount++
                    Log.d(TAG, "Synced expense: ${localExpense.id}")
                } else {
                    Log.e(TAG, "Failed to sync expense: ${localExpense.id}", result.exceptionOrNull())
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
     * Resolves conflicts between local and cloud expense data.
     * Cloud data takes precedence — any local expense matching a cloud record
     * by description and amount is marked as synced if it isn't already.
     * Reference: Firebase Realtime Database Android Docs - https://firebase.google.com/docs/database/android/read-and-write
     * Reference: IIE PROG7313 Module Manual (2026)
     */
    suspend fun resolveConflicts(): ConflictResolutionResult {
        return try {
            val cloudExpenses = remoteDb.observeExpenses().first()
            var conflictsResolved = 0

            for (cloudExpense in cloudExpenses) {
                val localMatches = localDb.expenseDao().getAllExpenses().first().filter {
                    it.description == cloudExpense.description &&
                            it.amount == cloudExpense.amount
                }
                for (local in localMatches) {
                    if (!local.syncedToCloud) {
                        val updated = local.copy(syncedToCloud = true)
                        localDb.expenseDao().update(updated)
                        conflictsResolved++
                    }
                }
            }

            Log.d(TAG, "Conflict resolution done: $conflictsResolved resolved")
            ConflictResolutionResult(conflictsResolved = conflictsResolved, success = true)
        } catch (e: Exception) {
            Log.e(TAG, "Conflict resolution failed", e)
            ConflictResolutionResult(success = false, error = e.message)
        }
    }

    sealed class SyncResult {
        data class Success(val count: Int) : SyncResult()
        data class Failure(val error: String) : SyncResult()
    }

    data class ConflictResolutionResult(
        val conflictsResolved: Int = 0,
        val success: Boolean = false,
        val error: String? = null
    )
}