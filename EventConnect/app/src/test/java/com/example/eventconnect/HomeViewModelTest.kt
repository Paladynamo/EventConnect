package com.example.eventconnect

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.eventconnect.ui.vm.HomeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private lateinit var vm: HomeViewModel

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        vm = HomeViewModel(app)
    }

    @Test
    fun loadAll_noCrash() = runBlocking {
        vm.loadAll()
        assertNotNull(vm.events.value)
    }
}
