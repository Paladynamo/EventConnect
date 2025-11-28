package com.example.eventconnect.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update

@Dao
interface PendingOperationDao {
    @Insert
    suspend fun insert(op: PendingOperation): Long

    @Query("SELECT * FROM pending_ops ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingOperation>

    @Delete
    suspend fun delete(op: PendingOperation)

    @Update
    suspend fun update(op: PendingOperation)

    @Query("DELETE FROM pending_ops WHERE id = :id")
    suspend fun deleteById(id: Long)
}

