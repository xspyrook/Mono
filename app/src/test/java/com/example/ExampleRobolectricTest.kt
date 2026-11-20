package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MONO GLASS", appName)
    }

    @Test
    fun `main activity declares home launcher intent filter`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val matches = context.packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        assertTrue(matches.any { it.activityInfo.packageName == context.packageName })
    }
}
