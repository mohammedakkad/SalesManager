package com.trader.salesmanager.util.export

import android.content.Context
import android.widget.Toast
import java.io.File

/** Keep the generated file available to the user when Android cannot save it. */
fun saveExportToDownloads(context: Context, file: File, displayName: String): Boolean {
    val saved = ExportManager.saveToDownloads(context, file, displayName)
    if (!saved) Toast.makeText(context, "تعذر حفظ الملف. حاول مرة أخرى.", Toast.LENGTH_LONG).show()
    return saved
}
