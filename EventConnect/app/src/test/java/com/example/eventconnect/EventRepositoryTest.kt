package com.example.eventconnect

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.Event
import com.example.eventconnect.repository.EventRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class EventRepositoryTest {
    private lateinit var repo: EventRepository

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        repo = EventRepository(AppDatabase.getDatabase(app).eventDao())
    }

    @After
    fun tearDown() {
        runBlocking { repo.clearAll() }
    }

    @Test
    fun initialSeed_eventsPresentOrEmpty() = runBlocking {
        val repo = EventRepository(app)
        val list = repo.getAll()
        assertNotNull(list)
        // Puede estar vacío si DB limpia, validar no nulo
    }

    @Test
    fun insertAndList() = runBlocking {
        repo.save(Event(title = "X", description = "", category = "", date = "2025-01-01", store = "", region = "", comuna = ""))
        val all = repo.list()
        assertEquals(1, all.size)
    }
}
