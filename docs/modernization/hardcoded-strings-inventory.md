# جرد النصوص العربية المكتوبة مباشرة في كود Kotlin

تقرير فقط (المرحلة 6B.5): لم يُنقل أي نص الآن. القاعدة من الآن: كل نص جديد يظهر للمستخدم يوضع في `strings.xml`.

المنهجية: عدد السلاسل النصية (string literals) في `src/main` التي تحتوي حرفاً عربياً، مع استبعاد التعليقات. تم توليده بسكربت Python بتاريخ 2026-11-22 على الفرع `chore/phase6-hardening`.

## الإجمالي حسب الوحدة

| الوحدة | عدد النصوص | عدد الملفات |
|---|---:|---:|
| `:app` | 42 | 13 |
| `:salesmanager` | 865 | 65 |
| `:admin` | 199 | 19 |
| **المجموع** | **1106** | **97** |

## حسب الملف (تنازلياً)

| الوحدة | الملف | العدد |
|---|---|---:|
| `:salesmanager` | `salesmanager/ui/reports/ReportsScreen.kt` | 77 |
| `:admin` | `admin/ui/merchants/detail/MerchantDetailScreen.kt` | 55 |
| `:salesmanager` | `salesmanager/util/pdf/InvoicePdfGenerator.kt` | 46 |
| `:salesmanager` | `salesmanager/util/export/ExportManager.kt` | 40 |
| `:salesmanager` | `salesmanager/ui/inventory/session/InventorySessionScreen.kt` | 39 |
| `:salesmanager` | `salesmanager/ui/settings/SettingsScreen.kt` | 37 |
| `:salesmanager` | `salesmanager/util/pdf/ReportPdfGenerator.kt` | 35 |
| `:salesmanager` | `salesmanager/ui/inventory/invoice/InvoiceItemsScreen.kt` | 34 |
| `:admin` | `admin/ui/subscriptionsettings/AdminSubscriptionScreen.kt` | 33 |
| `:salesmanager` | `salesmanager/ui/inventory/addedit/AddEditProductScreen.kt` | 31 |
| `:salesmanager` | `salesmanager/ui/transactions/details/TransactionDetailsScreen.kt` | 31 |
| `:salesmanager` | `salesmanager/ui/inventory/list/InventoryListScreen.kt` | 27 |
| `:salesmanager` | `salesmanager/ui/employees/EmployeeManagementScreen.kt` | 25 |
| `:salesmanager` | `salesmanager/ui/inventory/detail/ProductDetailScreen.kt` | 25 |
| `:salesmanager` | `salesmanager/ui/home/AnalyticsDashboard.kt` | 23 |
| `:salesmanager` | `salesmanager/ui/inventory/reports/StockReportsScreen.kt` | 23 |
| `:salesmanager` | `salesmanager/util/export/ExportActionButton.kt` | 23 |
| `:salesmanager` | `salesmanager/ui/chat/ChatScreen.kt` | 22 |
| `:salesmanager` | `salesmanager/util/InvoiceSharer.kt` | 22 |
| `:salesmanager` | `salesmanager/ui/transactions/addedit/AddEditTransactionScreen.kt` | 20 |
| `:salesmanager` | `salesmanager/util/pdf/CustomerStatementPdfGenerator.kt` | 19 |
| `:salesmanager` | `salesmanager/ui/reports/DayTransactionsScreen.kt` | 18 |
| `:salesmanager` | `salesmanager/ui/customers/addedit/AddEditCustomerScreen.kt` | 16 |
| `:salesmanager` | `salesmanager/ui/customers/list/CustomersScreen.kt` | 16 |
| `:salesmanager` | `salesmanager/ui/home/HomeScreen.kt` | 16 |
| `:admin` | `admin/ui/merchants/add/AddMerchantScreen.kt` | 16 |
| `:admin` | `admin/ui/merchants/list/MerchantsScreen.kt` | 16 |
| `:salesmanager` | `salesmanager/service/NotificationService.kt` | 15 |
| `:salesmanager` | `salesmanager/ui/returns/ReturnProcessScreen.kt` | 15 |
| `:admin` | `admin/ui/dashboard/DashboardScreen.kt` | 15 |
| `:salesmanager` | `salesmanager/ui/customers/details/CustomerDetailsScreen.kt` | 14 |
| `:admin` | `admin/ui/chat/detail/ChatDetailScreen.kt` | 14 |
| `:salesmanager` | `salesmanager/ui/customers/addedit/AddEditCustomerViewModel.kt` | 12 |
| `:app` | `core/worker/StatusCheckWorker.kt` | 9 |
| `:salesmanager` | `salesmanager/ui/transactions/list/TransactionsScreen.kt` | 9 |
| `:salesmanager` | `salesmanager/update/BackgroundUpdateWorker.kt` | 9 |
| `:salesmanager` | `salesmanager/util/export/ExportViewModel.kt` | 9 |
| `:app` | `core/util/ExpiryNotificationHelper.kt` | 7 |
| `:salesmanager` | `salesmanager/ui/activation/ActivationScreen.kt` | 7 |
| `:salesmanager` | `salesmanager/ui/settings/sessions/SessionsScreen.kt` | 7 |
| `:salesmanager` | `salesmanager/worker/UnpaidDebtWorker.kt` | 7 |
| `:admin` | `admin/ui/auth/LoginScreen.kt` | 7 |
| `:admin` | `admin/ui/merchants/add/AddMerchantViewModel.kt` | 7 |
| `:app` | `core/domain/model/ReturnModels.kt` | 6 |
| `:app` | `core/data/migration/FirestoreMigrationHelper.kt` | 5 |
| `:salesmanager` | `salesmanager/ui/activation/ActivationViewModel.kt` | 5 |
| `:salesmanager` | `salesmanager/ui/debts/DebtsScreen.kt` | 5 |
| `:salesmanager` | `salesmanager/ui/inventory/addedit/AddEditProductViewModel.kt` | 5 |
| `:salesmanager` | `salesmanager/ui/navigation/AppNavigation.kt` | 5 |
| `:salesmanager` | `salesmanager/ui/navigation/MainBottomNavigationBar.kt` | 5 |
| `:salesmanager` | `salesmanager/ui/navigation/SplashAndBlocked.kt` | 5 |
| `:salesmanager` | `salesmanager/ui/payments/PaymentMethodsScreen.kt` | 5 |
| `:salesmanager` | `salesmanager/ui/returns/ReturnViewModel.kt` | 5 |
| `:salesmanager` | `salesmanager/util/export/ExportState.kt` | 5 |
| `:admin` | `admin/AdminMessagingService.kt` | 5 |
| `:admin` | `admin/ui/chat/list/ChatListScreen.kt` | 5 |
| `:admin` | `admin/ui/subscriptionsettings/AdminSubscriptionViewModel.kt` | 5 |
| `:app` | `core/data/local/db/AppDatabase.kt` | 4 |
| `:salesmanager` | `salesmanager/ui/boxes/BoxesScreen.kt` | 4 |
| `:salesmanager` | `salesmanager/ui/employees/EmployeeManagementViewModel.kt` | 4 |
| `:salesmanager` | `salesmanager/ui/inventory/invoice/InvoiceUtils.kt` | 4 |
| `:salesmanager` | `salesmanager/ui/scanner/BarcodeScannerScreen.kt` | 4 |
| `:salesmanager` | `salesmanager/util/pdf/components/PdfStatusBadge.kt` | 4 |
| `:salesmanager` | `salesmanager/worker/LowStockCheckWorker.kt` | 4 |
| `:admin` | `admin/ui/notifications/NotificationsScreen.kt` | 4 |
| `:admin` | `admin/ui/requests/AdminRequestsScreen.kt` | 4 |
| `:app` | `core/domain/model/Product.kt` | 3 |
| `:salesmanager` | `salesmanager/ChatNotificationService.kt` | 3 |
| `:salesmanager` | `salesmanager/ui/chat/ChatViewModel.kt` | 3 |
| `:salesmanager` | `salesmanager/ui/components/SharedComponents.kt` | 3 |
| `:salesmanager` | `salesmanager/ui/transactions/addedit/AddEditTransactionViewModel.kt` | 3 |
| `:admin` | `admin/ui/auth/AuthViewModel.kt` | 3 |
| `:admin` | `admin/ui/notifications/NotificationsViewModel.kt` | 3 |
| `:app` | `core/data/repository/MerchantAdminRepositoryImpl.kt` | 2 |
| `:salesmanager` | `salesmanager/update/AppUpdateViewModel.kt` | 2 |
| `:salesmanager` | `salesmanager/util/pdf/PdfPrintManager.kt` | 2 |
| `:admin` | `admin/AdminAlertsWorker.kt` | 2 |
| `:admin` | `admin/ui/chat/detail/ChatDetailViewModel.kt` | 2 |
| `:admin` | `admin/ui/navigation/AdminNavigation.kt` | 2 |
| `:app` | `core/data/remote/ChatService.kt` | 1 |
| `:app` | `core/data/repository/EmployeeRepositoryImpl.kt` | 1 |
| `:app` | `core/data/repository/InventoryRepositoryImpl.kt` | 1 |
| `:app` | `core/domain/model/Customer.kt` | 1 |
| `:app` | `core/sync/SyncStatusObserver.kt` | 1 |
| `:app` | `core/util/format/AppNumberFormat.kt` | 1 |
| `:salesmanager` | `salesmanager/ui/inventory/detail/ProductDetailViewModel.kt` | 1 |
| `:salesmanager` | `salesmanager/ui/inventory/invoice/InvoiceItemsViewModel.kt` | 1 |
| `:salesmanager` | `salesmanager/ui/reports/ReportsViewModel.kt` | 1 |
| `:salesmanager` | `salesmanager/ui/settings/backup/BackupManager.kt` | 1 |
| `:salesmanager` | `salesmanager/ui/settings/sessions/SessionsViewModel.kt` | 1 |
| `:salesmanager` | `salesmanager/util/export/ExportDownload.kt` | 1 |
| `:salesmanager` | `salesmanager/util/pdf/components/PdfFooter.kt` | 1 |
| `:salesmanager` | `salesmanager/util/pdf/components/PdfHeader.kt` | 1 |
| `:salesmanager` | `salesmanager/util/pdf/components/PdfLogo.kt` | 1 |
| `:salesmanager` | `salesmanager/util/pdf/components/PdfTable.kt` | 1 |
| `:salesmanager` | `salesmanager/util/pdf/core/PdfTextPainter.kt` | 1 |
| `:admin` | `admin/ui/requests/AdminRequestsViewModel.kt` | 1 |
