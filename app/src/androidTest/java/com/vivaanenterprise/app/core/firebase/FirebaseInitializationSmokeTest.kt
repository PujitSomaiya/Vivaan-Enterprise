package com.vivaanenterprise.app.core.firebase

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirebaseInitializationSmokeTest {

    @Test
    fun testFirebaseAppInitializesSuccessfully() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        FirebaseApp.initializeApp(appContext)
        val apps = FirebaseApp.getApps(appContext)
        assertTrue("FirebaseApp default instance should initialize and not be empty", apps.isNotEmpty())
    }
}
