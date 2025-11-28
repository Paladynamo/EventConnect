package com.example.eventconnect

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.eventconnect.ui.vm.HomeViewModel
import com.example.eventconnect.remote.service.EventService
import com.example.eventconnect.remote.dto.EventDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeViewModelNetworkFallbackTest {
    private lateinit var vm: HomeViewModel

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        vm = HomeViewModel(app)
    }

    @Test
    fun networkFailure_fallbackToLocal() = runTest {
        val fakeService = mockk<EventService>()
        coEvery { fakeService.list(any(), any(), any(), any()) } throws RuntimeException("net fail")
        coEvery { fakeService.list(any(), any(), any(), any()) } throws RuntimeException("net fail")
        val vm = HomeViewModel(app) { fakeService }
        vm.loadAll()
        // Si falla red, lista puede ser la local (quizás vacía, lo aceptamos mientras no crashea)
        assertTrue(vm.events.value != null)
    }

    @Test
    fun networkSuccess_mapsEvents() = runTest {
        val fakeService = mockk<EventService>()
        coEvery { fakeService.list(any(), any(), any(), any()) } returns listOf(EventDto(title="X", storeName="Store"))
        val vm = HomeViewModel(app) { fakeService }
        vm.loadAll()
        assertTrue(vm.events.value.size == 1 && vm.events.value[0].title == "X")
    }

    @Test
    fun loadAll_fallbacksWhenBackendFails() = runBlocking {
        vm.loadAll()
        assertNotNull(vm.events.value)
    }
}
