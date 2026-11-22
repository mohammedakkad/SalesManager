package com.trader.salesmanager.qa

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** Fail before app/provider initialization unless the APK contains the offline CI fixture. */
class OfflineQaRunner:AndroidJUnitRunner() {
    override fun newApplication(cl:ClassLoader,className:String,context:Context):Application {
        val id=context.resources.getIdentifier("project_id","string",context.packageName)
        // ".test" is the minified staging build used only by the R8 smoke lane (-PsellerTestBuildType=staging).
        val minifiedSmoke=context.packageName.endsWith(".test")
        check((context.packageName.endsWith(".debug") || minifiedSmoke) && id!=0 && context.getString(id)=="seller-qa-offline") {
            "Refusing instrumentation against a real Firebase configuration; generate the offline QA fixture first."
        }
        // R8 smoke: start the real Application (Koin, Room, WorkManager, Firebase) from the minified APK.
        if (minifiedSmoke) return super.newApplication(cl,className,context)
        // No Seller Application/Koin/workers/FCM startup. Screens receive isolated repositories.
        return super.newApplication(cl,Application::class.java.name,context)
    }
}
