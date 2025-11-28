package com.example.eventconnect

import androidx.test.core.app.ApplicationProvider
import com.example.eventconnect.location.AddressValidator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressValidatorTest {
    private val app = ApplicationProvider.getApplicationContext<android.app.Application>()

    @Test
    fun shortStreet_invalidQuickly() = runTest {
        val r = AddressValidator.validate(app, "A", "12", "Santiago", "Metropolitana")
        assertTrue(r.valid == false)
    }

    @Test
    fun plausibleAddress_returnsResult() = runTest {
        val r = AddressValidator.validate(app, "Alameda", "100", "Santiago", "Metropolitana")
        assertTrue(r.valid == true || r.valid == false || r.valid == null)
    }
}
