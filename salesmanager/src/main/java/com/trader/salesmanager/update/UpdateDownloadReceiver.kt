package com.trader.salesmanager.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UpdateDownloadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val url = intent.getStringExtra(BackgroundUpdateWorker.KEY_URL) ?: return
        val versionName = intent.getStringExtra(BackgroundUpdateWorker.KEY_VERSION_NAME) ?: ""
        BackgroundUpdateWorker.scheduleDownload(context, url, versionName)
    }
}
