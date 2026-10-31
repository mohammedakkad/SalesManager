package com.trader.salesmanager.qa

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** Fail before app/provider initialization unless the APK contains the offline CI fixture. */
class OfflineQaRunner:AndroidJUnitRunner() {
    override fun newApplication(cl:ClassLoader,className:String,context:Context):Application {
        val id=context.resources.getIdentifier("project_id","string",context.packageName)
        check(context.packageName.endsWith(".debug") && id!=0 && context.getString(id)=="seller-qa-offline") {
            "Refusing instrumentation against a real Firebase configuration; generate the offline QA fixture first."
        }
        // No Seller Application/Koin/workers/FCM startup. Screens receive isolated repositories.
        return super.newApplication(cl,Application::class.java.name,context)
    }
}
