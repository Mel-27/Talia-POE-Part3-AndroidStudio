package com.gia.poe_demo.data.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.gia.poe_demo.data.remote.CategoryModel
import com.gia.poe_demo.data.remote.ExpenseModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Realtime Database Manager
 *
 * Handles all read/write operations to Firebase Realtime Database.
 * Provides real-time observation using Kotlin Flow.
 *
 * Database structure:
 * - users/{userId}/expenses/{expenseId}
 * - users/{userId}/categories/{categoryId}
 *
 * Reference:
 * - https://firebase.google.com/docs/database/android/read-and-write
 * - https://developer.android.com/kotlin/flow
 */

class RealtimeDbManager {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "RealtimeDbManager"
    }

    /**
     * Get current authenticated user ID
     * Returns null if no user is logged in
     */
    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Get reference to user's expenses node
     * Structure: users/{userId}/expenses/
     */
    private fun getUserExpensesRef(): DatabaseReference? {
        val userId = getCurrentUserId() ?: return null
        return database.child("users").child(userId).child("expenses")
    }

    /**
     * Get reference to user's categories node
     * Structure: users/{userId}/categories/
     */
    private fun getUserCategoriesRef(): DatabaseReference? {
        val userId = getCurrentUserId() ?: return null
        return database.child("users").child(userId).child("categories")
    }

    // ============================================================
    // WRITE OPERATIONS
    // ============================================================

    /**
     * Save expense to Firebase Realtime Database
     *
     * @param expense The expense model to save
     * @return Result containing the Firebase key or error
     */
    suspend fun saveExpense(expense: ExpenseModel): Result<String> {
        return try {
            val expensesRef = getUserExpensesRef()
            if (expensesRef == null) {
                return Result.failure(Exception("User not logged in"))
            }

            // Generate unique key for the expense
            val expenseId = expensesRef.push().key
                ?: return Result.failure(Exception("Failed to generate key"))

            val expenseWithId = expense.copy(
                id = expenseId,
                userId = getCurrentUserId() ?: ""
            )

            // Save to database
            expensesRef.child(expenseId).setValue(expenseWithId).await()

            Log.d(TAG, "Expense saved to Realtime DB: $expenseId")
            Result.success(expenseId)

        } catch (e: Exception) {
            Log.e(TAG, "Error saving expense", e)
            Result.failure(e)
        }
    }

    /**
     * Save category to Firebase Realtime Database
     *
     * @param category The category model to save
     * @return Result containing the Firebase key or error
     */
    suspend fun saveCategory(category: CategoryModel): Result<String> {
        return try {
            val categoriesRef = getUserCategoriesRef()
            if (categoriesRef == null) {
                return Result.failure(Exception("User not logged in"))
            }

            val categoryId = categoriesRef.push().key
                ?: return Result.failure(Exception("Failed to generate key"))

            val categoryWithId = category.copy(
                id = categoryId,
                userId = getCurrentUserId() ?: ""
            )

            categoriesRef.child(categoryId).setValue(categoryWithId).await()

            Log.d(TAG, "Category saved to Realtime DB: $categoryId")
            Result.success(categoryId)

        } catch (e: Exception) {
            Log.e(TAG, "Error saving category", e)
            Result.failure(e)
        }
    }

    /**
     * Updates an existing expense
     *
     * @param expenseId The Firebase key of the expense to update
     * @param updates Map of field names to new values
     */
    suspend fun updateExpense(expenseId: String, updates: Map<String, Any>): Result<Boolean> {
        return try {
            val expensesRef = getUserExpensesRef()
            if (expensesRef == null) {
                return Result.failure(Exception("User not logged in"))
            }

            expensesRef.child(expenseId).updateChildren(updates).await()
            Log.d(TAG, "Expense updated: $expenseId")
            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error updating expense", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes an expense from Firebase Realtime Database
     *
     * @param expenseId The Firebase key of the expense to delete
     */
    suspend fun deleteExpense(expenseId: String): Result<Boolean> {
        return try {
            val expensesRef = getUserExpensesRef()
            if (expensesRef == null) {
                return Result.failure(Exception("User not logged in"))
            }

            expensesRef.child(expenseId).removeValue().await()
            Log.d(TAG, "Expense deleted: $expenseId")
            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error deleting expense", e)
            Result.failure(e)
        }
    }

    // ============================================================
    // READ OPERATIONS (REAL-TIME)
    // ============================================================

    /**
     * Observe expenses in real-time using Kotlin Flow
     *
     * This creates a real-time listener that emits new data
     * whenever expenses change in Firebase.
     *
     * The Flow will automatically clean up the listener when
     * the collection is cancelled.
     */
    fun observeExpenses(): Flow<List<ExpenseModel>> = callbackFlow {
        val expensesRef = getUserExpensesRef()

        if (expensesRef == null) {
            close(Exception("User not logged in"))
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val expensesList = mutableListOf<ExpenseModel>()
                for (childSnapshot in snapshot.children) {
                    val expense = childSnapshot.getValue(ExpenseModel::class.java)
                    expense?.let { expensesList.add(it) }
                }
                // Sort by date (newest first)
                val sortedExpenses = expensesList.sortedByDescending { it.date }
                Log.d(TAG, "Loaded ${sortedExpenses.size} expenses from Realtime DB")
                trySend(sortedExpenses)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error observing expenses", error.toException())
                close(error.toException())
            }
        }

        expensesRef.addValueEventListener(listener)

        awaitClose {
            expensesRef.removeEventListener(listener)
            Log.d(TAG, "Expense observation stopped")
        }
    }

    /**
     * Observe categories in real-time using Kotlin Flow
     */
    fun observeCategories(): Flow<List<CategoryModel>> = callbackFlow {
        val categoriesRef = getUserCategoriesRef()

        if (categoriesRef == null) {
            close(Exception("User not logged in"))
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val categoriesList = mutableListOf<CategoryModel>()
                for (childSnapshot in snapshot.children) {
                    val category = childSnapshot.getValue(CategoryModel::class.java)
                    category?.let { categoriesList.add(it) }
                }
                val sortedCategories = categoriesList.sortedBy { it.name }
                Log.d(TAG, "Loaded ${sortedCategories.size} categories from Realtime DB")
                trySend(sortedCategories)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error observing categories", error.toException())
                close(error.toException())
            }
        }

        categoriesRef.addValueEventListener(listener)

        awaitClose {
            categoriesRef.removeEventListener(listener)
            Log.d(TAG, "Category observation stopped")
        }
    }

    /**
     * Gets a single expense once
     */
    suspend fun getExpense(expenseId: String): ExpenseModel? {
        return try {
            val expensesRef = getUserExpensesRef()
            if (expensesRef == null) return null

            val snapshot = expensesRef.child(expenseId).get().await()
            snapshot.getValue(ExpenseModel::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting expense", e)
            null
        }
    }

    // ============================================================
    // SYNC HELPER METHODS
    // ============================================================

    /**
     * Syncs a single expense to cloud
     */
    suspend fun syncExpenseToCloud(expense: ExpenseModel): Boolean {
        return try {
            val result = saveExpense(expense)
            result.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing expense", e)
            false
        }
    }

    /**
     * Syncs multiple expenses to cloud (batch operation)
     *
     * @param expenses List of expenses to sync
     * @return Number of successfully synced expenses
     */
    suspend fun syncAllExpensesToCloud(expenses: List<ExpenseModel>): Int {
        var successCount = 0
        for (expense in expenses) {
            if (syncExpenseToCloud(expense)) {
                successCount++
            }
        }
        Log.d(TAG, "Synced $successCount/${expenses.size} expenses to cloud")
        return successCount
    }
}

/*
References:

Firebase, 2026. Read and Write Data on Android.
Available at: https://firebase.google.com/docs/database/android/read-and-write
[Accessed 25 May 2026].

Firebase, 2026. Work with Lists of Data on Android.
Available at: https://firebase.google.com/docs/database/android/lists-of-data
[Accessed 25 May 2026].

Firebase, 2026. Structure Your Database.
Available at: https://firebase.google.com/docs/database/android/structure-data
[Accessed 25 May 2026].

Android Developers, 2024. Kotlin Flow on Android.
Available at: https://developer.android.com/kotlin/flow
[Accessed 25 May 2026].
*/