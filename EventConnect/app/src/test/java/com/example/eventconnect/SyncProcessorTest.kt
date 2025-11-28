package com.example.eventconnect

import androidx.test.core.app.ApplicationProvider
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.PendingOperation
import com.example.eventconnect.sync.SyncProcessor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncProcessorTest {
    private val app: android.app.Application = ApplicationProvider.getApplicationContext()

    @Before
    fun clearDb() = runTest {
        AppDatabase.getDatabase(app).pendingOperationDao().deleteAll()
    }

    @Test
    fun unknownOperation_isRetained() = runTest {
        val processor = SyncProcessor(app)
        val dao = AppDatabase.getDatabase(app).pendingOperationDao()
        val op = PendingOperation(type = "UNKNOWN", payload = "{}")
        dao.insert(op)
        val pending = dao.getAll()
        val remaining = processor.process(pending)
        assertTrue(remaining.isNotEmpty())
    }

    @Test
    fun reservationApprove_processed() = runTest {
        val processor = SyncProcessor(app)
        val op = PendingOperation(id = 2, type = "RESERVATION_APPROVE", payload = "{\"reservationId\":999}")
        val remaining = processor.process(listOf(op))
        assertEquals(0, remaining.size)
    }
}
