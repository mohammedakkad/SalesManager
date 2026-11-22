package com.trader.admin.ui

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import org.robolectric.Shadows.shadowOf

/** Registers a plain ComponentActivity with Robolectric's package manager before the compose rule starts it. */
object AdminTestActivity {
    fun rule(): TestRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                val context = ApplicationProvider.getApplicationContext<Application>()
                val pm = shadowOf(context.packageManager)
                val info = pm.addActivityIfNotPresent(ComponentName(context, ComponentActivity::class.java))
                info.applicationInfo = context.applicationInfo; info.exported = true
                info.theme = android.R.style.Theme_Material_Light_NoActionBar
                pm.addOrUpdateActivity(info)
                base.evaluate()
            }
        }
    }
}
