package com.gia.poe_demo.data.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.gia.poe_demo.data.remote.CategoryModel
import com.gia.poe_demo.data.remote.ExpenseModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * RealtimeDbManager — handles all read and write operations
 * with Firebase Realtime Database for expenses and categories.
 * Reference: Firebase Realtime Database Android Docs - https://firebase.google.com/docs/database/android/start
 * Reference: Firebase Realtime Database Read and Write - https://firebase.google.com/docs/database/android/read-and-write
 * Reference: IIE PROG7313 Module Manual (2026)
 */
class RealtimeDbManager {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "RealtimeDbManager"
    }

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    private fun getUserExpensesRef(): DatabaseReference? {
        val userId = getCurrentUserId() ?: return null
        return database.child("users").child(userId).child("expenses")
    }

    private fun getUserCategoriesRef(): DatabaseReference? {
        val userId = getCurrentUserId() ?: return null
        return database.child("users").child(userId).child("categories")
    }

    suspend fun saveExpense(expense: ExpenseModel): Result<String> {
        return try {
            val expensesRef = getUserExpensesRef()
                ?: return Result.failure(Exception("User not logged in"))

            val expenseId = expensesRef.push().key
                ?: return Result.failure(Exception("Failed to generate key"))

            val expenseWithId = expense.copy(
                id = expenseId,
                userId = getCurrentUserId() ?: ""
            )

            expensesRef.child(expenseId).setValue(expenseWithId).await()
            Log.d(TAG, "Expense saved: $expenseId")
            Result.success(expenseId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving expense", e)
            Result.failure(e)
        }
    }

    suspend fun saveCategory(category: CategoryModel): Result<String> {
        return try {
            val categoriesRef = getUserCategoriesRef()
                ?: return Result.failure(Exception("User not logged in"))

            val categoryId = categoriesRef.push().key
                ?: return Result.failure(Exception("Failed to generate key"))

            val categoryWithId = category.copy(
                id = categoryId,
                userId = getCurrentUserId() ?: ""
            )

            categoriesRef.child(categoryId).setValue(categoryWithId).await()
            Log.d(TAG, "Category saved: $categoryId")
            Result.success(categoryId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving category", e)
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Boolean> {
        return try {
            val expensesRef = getUserExpensesRef()
                ?: return Result.failure(Exception("User not logged in"))

            expensesRef.child(expenseId).removeValue().await()
            Log.d(TAG, "Expense deleted: $expenseId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting expense", e)
            Result.failure(e)
        }
    }

    fun observeExpenses(): Flow<List<ExpenseModel>> = callbackFlow {
        val expensesRef = getUserExpensesRef()

        if (expensesRef == null) {
            close(Exception("User not logged in"))
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ExpenseModel>()
                for (child in snapshot.children) {
                    val expense = child.getValue(ExpenseModel::class.java)
                    expense?.let { list.add(it) }
                }
                val sorted = list.sortedByDescending { it.date }
                Log.d(TAG, "Loaded ${sorted.size} expenses from Realtime DB")
                trySend(sorted)
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

    fun observeCategories(): Flow<List<CategoryModel>> = callbackFlow {
        val categoriesRef = getUserCategoriesRef()

        if (categoriesRef == null) {
            close(Exception("User not logged in"))
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<CategoryModel>()
                for (child in snapshot.children) {
                    val category = child.getValue(CategoryModel::class.java)
                    category?.let { list.add(it) }
                }
                val sorted = list.sortedBy { it.name }
                Log.d(TAG, "Loaded ${sorted.size} categories from Realtime DB")
                trySend(sorted)
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
}