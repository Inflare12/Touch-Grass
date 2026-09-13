package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.blocking.AdBypassManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdBypassManagerTest {
    @Test
    fun bypass_is_persisted_and_expires() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = AdBypassManager(context)
        val packageName = "com.example.testbypass"
        manager.grant(packageName, durationMillis = 10_000L)

        assertTrue(manager.isActive(packageName))
        assertFalse(manager.isActive(packageName, System.currentTimeMillis() + 11_000L))
    }
}
