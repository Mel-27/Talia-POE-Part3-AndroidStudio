package com.gia.poe_demo.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.gia.poe_demo.data.entities.Reminder

@Dao
interface ReminderDao {

    @Insert
    suspend fun insert(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Update
    suspend fun update(reminder: Reminder)

    @Query("SELECT * FROM reminders ORDER BY date ASC")
    fun getAll(): LiveData<List<Reminder>>
}