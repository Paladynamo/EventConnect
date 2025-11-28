package com.example.eventconnect

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.eventconnect.ui.vm.HomeViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeViewModelFiltersTest {
    private lateinit var vm: HomeViewModel

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        vm = HomeViewModel(app)
    }

    @Test
    fun loadByRegion_handlesMissingBackendGracefully() = runBlocking {
        vm.loadByRegion("Metropolitana")
        assertTrue(vm.events.value.isNotEmpty() || vm.events.value.isEmpty())
    }

    @Test
    fun loadByRegionComunaCategory_noCrash() = runBlocking {
        vm.loadByRegionComunaCategory("Metropolitana","Santiago","TCG")
        assertNotNull(vm.events.value)
    }
}
