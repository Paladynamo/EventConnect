package com.example.eventconnect.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.eventconnect.data.AppDatabase

class PendingSyncWorker(appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val ops = db.pendingOperationDao().getAll()
        if (ops.isEmpty()) return Result.success()
        val processor = SyncProcessor(applicationContext)
        val remaining = processor.process(ops)
        val dao = db.pendingOperationDao()
        // limpiar todos y reinsertar los restantes
        ops.forEach { dao.delete(it) }
        remaining.forEach { dao.insert(it) }
        return Result.success()
    }
}
