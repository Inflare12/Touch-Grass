package com.example

import android.content.Context
import com.example.blocking.AdBypassManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class AdBypassManagerTest {
    @Test
    fun bypass_is_active_then_expires() {
        val context = RuntimeEnvironment.getApplication() as Context
        val manager = AdBypassManager(context)
        val packageName = "com.example.testbypass"
        manager.grant(packageName, durationMillis = 10_000L)

        assertTrue(manager.isActive(packageName))
        assertFalse(manager.isActive(packageName, System.currentTimeMillis() + 11_000L))
    }
}
