package com.example.eventconnect

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.eventconnect.remote.sync.RemoteReservationSync
import com.example.eventconnect.data.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteReservationSyncTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun changeStatus_offlineQueuesPending() = runTest {
        val sync = RemoteReservationSync(app)
        // Forzamos fallo apagando red (simulado: no token -> 401) usando id ficticio
        sync.changeStatus(9999, "APPROVED")
        val pending = AppDatabase.getDatabase(app).pendingOperationDao().getAll().any { it.type == "RESERVATION_STATUS" }
        assertTrue(pending)
    }

    @Test
    fun enqueueWhenNetworkFails_doesNotCrash() = runTest {
        val sync = RemoteReservationSync(app)
        sync.changeStatus(1, "APPROVED")
    }
}
