package com.trader.salesmanager.util.export

/**
 * حالات التصدير — نموذج أحادي المصدر للحقيقة.
 * كل حالة معبّرة عن نفسها ولا تحتاج تعليق.
 */
sealed interface ExportState {
    /** لا يوجد تصدير جارٍ */
    data object Idle : ExportState

    /** جارٍ التحضير أو الكتابة */
    data class Loading(val message: String = "جارٍ التحضير...") : ExportState

    /** اكتمل التصدير */
    data class Success(
        val fileName: String,
        val filePath: String,
        val type: ExportType,
        val savedToDownloads: Boolean = false
    ) : ExportState

    /** فشل التصدير */
    data class Error(val message: String, val cause: Throwable? = null) : ExportState
}

enum class ExportType(val mimeType: String, val ext: String, val label: String) {
    PDF ("application/pdf",                                                         "pdf",  "PDF"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx", "Excel")
}

enum class ExportTarget(val label: String, val icon: String) {
    INVOICE_PDF       ("فاتورة PDF",         "📄"),
    CUSTOMER_STATEMENT("كشف حساب PDF",       "👤"),
    SALES_REPORT_EXCEL("تقرير المبيعات Excel","📊"),
    INVENTORY_EXCEL   ("تقرير المخزون Excel", "📦")
}
