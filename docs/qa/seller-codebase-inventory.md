> Current continuation (2026-11-11):632 JVM PASS (31 new),22 Python guards PASS (10 new). Hosted42983779778 SUCCESS: seller and compose API24/28/33/35. Seller Android+JVM lines85.0584%; weighted historical scope82.8749%; additional Android+both-JVM-suites83.9419%. Goal95% NOT ACHIEVED; releaseFAIL. [Current results and scope definitions](coverage95-next-progress.md). Earlier figures below are historical.

# Seller codebase inventory — Stage A


Historical qualification (2026-11-09):audited sourceef1c8999577518e14f1fc799328360eb38721874. [Hosted run42693672936](https://github.com/mohammedakkad/SalesManager/actions/runs/42693672936):workflow/seller/composeSUCCESS;7 actual Android and5 actual PDFBox casesPASS. Local app/Seller409 JVM and11 Python guardsPASS. JVM Seller74.5680%, weighted74.3424%; additional hosted Android+JVM Seller80.2009%, weighted77.9767%. Target100%NOT ACHIEVED; release readinessFAIL. Remaining69 defined work items:36NOT TESTED,32BLOCKED,1PARTIAL. [Detailed Arabic accounting](test-completion-status-ar.md), [structured evidence](evidence/continuation/coverage100/final/qualification.json). Historical stages below retain their original outcomes.

Starting production SHA: `22381d5b55fb59a9270932bcf720b283fa1c90ff` (freshly fetched).
Branch: `codex/seller-full-coverage-audit`. Initial tree clean. Admin business logic is outside scope.

## Architecture (verified by source)
`:salesmanager` is the Seller Android application; `:app` is now a shared Android library (not the application on main). `:admin` consumes the same library but is outside the audit scope. Kotlin 2.0.21, AGP 8.3.2, KSP 2.0.21-1.0.27, Compose, Room 2.6.1, coroutines 1.9.0, Koin 3.5.6, Firebase BOM 33.4.0. compile/target SDK 35, min SDK 24. Debug, staging, release variants; staging and release use R8 and release signing.

Startup: SalesManagerApp initializes FCM/Crashlytics/Realtime Database persistence, Koin salesManagerModule, debt/status/update workers, and SyncCoordinator. MainActivity enters AppNavigation; startup/activation gates precede merchant screens. Koin provides database DAOs, concrete remote services, repository interfaces, parameterized ViewModels, worker dependencies, and a runBlocking merchantId singleton.

Navigation/features verified: activation/blocking, dashboard/sync status, customers, transactions/invoice items, debts/reminders, payment methods/cash boxes, inventory/product units/counting/stock reports, returns, analytics/day transactions, barcode camera, chat, settings, sessions/employees, encrypted backup/restore, PDF export/print, APK updates. Feature correctness is not established by inventory.

## Baseline
At initial production discovery no test source sets/coverage configuration were present. Current state:104 passing JVM tests,4 compiled instrumentation tests,and2 host PDF-content tests awaiting device fixtures. Initial production coverage was **not measured**; current measured counters are in seller-coverage-report.md. All200 production Kotlin/Java source files are inventoried; this does not imply deep behavioral review of every method. No historical schemas were present initially; actual pre-change22 and current23 compiler exports are now committed. Baseline database version22 had migrations1→22 and exportSchema=false. Continuation exports real22/23 schemas and adds the verified22→23 migration; older schemas remain absent. No destructive fallback found in its builder.

Baseline command: `/workspace/setup/gradle --no-daemon --max-workers=4 :salesmanager:tasks --all` failed during `:admin` configuration: missing signing values cast to non-null String (admin/build.gradle.kts:26). Seller has the same unsafe pattern. No tests ran. Logs: `/workspace/setup/production-baseline.log` (local).

## Risk and test strategy
P0 candidates: remote snapshot deletes unsynced local transactions/returns; migration table rebuild and FK behavior; non-finite return values; snapshot/restore consistency; financial writes across repository boundaries. These require regression reproduction before asserting runtime effects. P1: activation listener cancellation, stale merchant singleton, retry/failed sync handling, double returns, stock/cash consistency. P2: locale/RTL formatting, navigation escaping, long lists, export and update error handling.

Required journeys: activation→customer→sale→cash/debt; partial/full return→stock/refund; offline sale→reconnect; count adjustment; backup→isolated restore→record comparison. All require isolated fixtures/fakes; never live merchant data. External boundaries: Firebase Auth/Realtime DB/Firestore/FCM/Remote Config/Crashlytics, Cloudinary, GitHub updates, Android camera/notifications/storage/print.

Use JUnit, coroutine test dispatchers, mocks only at external boundaries; Robolectric for Room/integration; Compose instrumentation on an emulator for end-to-end behavior. Add JaCoCo XML/HTML without production class exclusions. Do not derive gates from unmeasured coverage. Historical migration tests require authentic schemas; current in-memory DB tests do not validate upgrades.

## Complete source enumeration
This is a discovery inventory, not a claim that every method has received a deep correctness review. Each source is listed with declared types; top-level functions remain included by file.

| Source | Types |
| --- | --- |
| `app/src/main/java/com/trader/core/data/local/DataStoreProvider.kt` |  |
| `app/src/main/java/com/trader/core/data/local/dao/CashBoxDao.kt` | CashBoxDao |
| `app/src/main/java/com/trader/core/data/local/dao/CashBoxMovementDao.kt` | CashBoxMovementDao |
| `app/src/main/java/com/trader/core/data/local/dao/CustomerDao.kt` | CustomerDao |
| `app/src/main/java/com/trader/core/data/local/dao/EmployeeDao.kt` | EmployeeDao |
| `app/src/main/java/com/trader/core/data/local/dao/InventoryDao.kt` | FinancialReportTotalsProjection, DailySalesProfitProjection, TopSellingProductProjection, InvoiceItemDao, InventoryDao |
| `app/src/main/java/com/trader/core/data/local/dao/LowStockAlertDao.kt` | LowStockAlertDao |
| `app/src/main/java/com/trader/core/data/local/dao/PaymentMethodDao.kt` | PaymentMethodDao |
| `app/src/main/java/com/trader/core/data/local/dao/PendingMessageDao.kt` | PendingMessageDao |
| `app/src/main/java/com/trader/core/data/local/dao/ProductDao.kt` | ProductWithUnitsRelation, InventoryValueProjection, ProductDao |
| `app/src/main/java/com/trader/core/data/local/dao/ReturnDao.kt` | ReturnDao, UnitReturnSummary |
| `app/src/main/java/com/trader/core/data/local/dao/SessionDao.kt` | SessionDao |
| `app/src/main/java/com/trader/core/data/local/dao/StockMovementDao.kt` | StockMovementDao |
| `app/src/main/java/com/trader/core/data/local/dao/TransactionDao.kt` | DebtAgingProjection, TodaySalesSummaryProjection, TopDebtorCustomerProjection, DailySalesProjection, TransactionDao |
| `app/src/main/java/com/trader/core/data/local/db/AppDatabase.kt` | AppDatabase |
| `app/src/main/java/com/trader/core/data/local/entity/CashBoxEntity.kt` | CashBoxEntity |
| `app/src/main/java/com/trader/core/data/local/entity/CashBoxMovementEntity.kt` | CashBoxMovementEntity |
| `app/src/main/java/com/trader/core/data/local/entity/CustomerEntity.kt` | CustomerEntity |
| `app/src/main/java/com/trader/core/data/local/entity/EmployeeEntity.kt` | EmployeeEntity |
| `app/src/main/java/com/trader/core/data/local/entity/InvoiceAndInventoryEntities.kt` | InvoiceItemEntity, InventorySessionEntity, InventorySessionItemEntity |
| `app/src/main/java/com/trader/core/data/local/entity/LowStockAlertStateEntity.kt` | LowStockAlertStateEntity |
| `app/src/main/java/com/trader/core/data/local/entity/PaymentMethodEntity.kt` | PaymentMethodEntity |
| `app/src/main/java/com/trader/core/data/local/entity/PendingMessageEntity.kt` | PendingMessageEntity |
| `app/src/main/java/com/trader/core/data/local/entity/ProductEntity.kt` | ProductEntity, ProductUnitEntity |
| `app/src/main/java/com/trader/core/data/local/entity/ReturnEntities.kt` | ReturnInvoiceEntity, ReturnItemEntity |
| `app/src/main/java/com/trader/core/data/local/entity/SessionEntity.kt` | SessionEntity |
| `app/src/main/java/com/trader/core/data/local/entity/StockMovementEntity.kt` | StockMovementEntity |
| `app/src/main/java/com/trader/core/data/local/entity/TransactionEntity.kt` | TransactionEntity |
| `app/src/main/java/com/trader/core/data/manager/EmployeeSessionManager.kt` | EmployeeSessionManager |
| `app/src/main/java/com/trader/core/data/migration/FirestoreMigrationHelper.kt` | FirestoreMigrationHelper |
| `app/src/main/java/com/trader/core/data/remote/ChatService.kt` | ChatService |
| `app/src/main/java/com/trader/core/data/remote/CloudinaryUploader.kt` | CloudinaryUploader, CloudinaryUploadException |
| `app/src/main/java/com/trader/core/data/remote/FirebaseSyncService.kt` | FirebaseSyncService, MerchantData, ValidationResult, Active, Disabled, Expired, NotFound, NetworkError |
| `app/src/main/java/com/trader/core/data/remote/MerchantAdminService.kt` | MerchantAdminService |
| `app/src/main/java/com/trader/core/data/remote/ProductFirestoreService.kt` | ProductFirestoreService |
| `app/src/main/java/com/trader/core/data/repository/ActivationRepositoryImpl.kt` | ActivationRepositoryImpl |
| `app/src/main/java/com/trader/core/data/repository/AdminSubscriptionSettingsRepositoryImpl.kt` | AdminSubscriptionSettingsRepositoryImpl |
| `app/src/main/java/com/trader/core/data/repository/CashBoxRepositoryImpl.kt` | CashBoxRepositoryImpl |
| `app/src/main/java/com/trader/core/data/repository/ChatRepositoryImpl.kt` | ChatRepositoryImpl |
| `app/src/main/java/com/trader/core/data/repository/CustomerRepositoryImpl.kt` | CustomerRepositoryImpl |
| `app/src/main/java/com/trader/core/data/repository/DashboardAnalyticsRepositoryImpl.kt` | DashboardAnalyticsRepositoryImpl |
| `app/src/main/java/com/trader/core/data/repository/EmployeeRepositoryImpl.kt` | EmployeeRepositoryImpl |
| `app/src/main/java/com/trader/core/data/repository/FirebaseSubscriptionRepositoryImpl.kt` | FirebaseSubscriptionRepositoryImpl |
| `app/src/main/java/com/trader/core/data/repository/InventoryRepositoryImpl.kt` | InventoryRepositoryImpl |
| `app/src/main/java/com/trader/core/data/repository/InvoiceItemRepositoryImpl.kt` | InvoiceItemRepositoryImpl |
| `app/src/main/java/com/trader/core/data/repository/LowStockAlertRepositoryImpl.kt` | LowStockAlertRepositoryImpl |
| `app/src/main/java/com/trader/core/data/repository/LowStockSettingsRepositoryImpl.kt` | LowStockSettingsRepositoryImpl |
| `app/src/main/java/com/trader/core/data/repository/MerchantAdminRepositoryImpl.kt` | MerchantAdminRepositoryImpl |
| `app/src/main/java/com/trader/core/data/repository/MerchantStatusRepositoryImpl.kt` | MerchantStatusRepositoryImpl |
| `app/src/main/java/com/trader/core/data/repository/PaymentMethodRepositoryImpl.kt` | PaymentMethodRepositoryImpl |
| `app/src/main/java/com/trader/core/data/repository/ProductRepositoryImpl.kt` | ProductRepositoryImpl |
| `app/src/main/java/com/trader/core/data/repository/ReportsRepositoryImpl.kt` | ReportsRepositoryImpl |
| `app/src/main/java/com/trader/core/data/repository/ReturnRepositoryImpl.kt` | ReturnRepositoryImpl |
| `app/src/main/java/com/trader/core/data/repository/StockRepositoryImpl.kt` | StockRepositoryImpl, StockConflict |
| `app/src/main/java/com/trader/core/data/repository/TransactionRepositoryImpl.kt` | TransactionRepositoryImpl |
| `app/src/main/java/com/trader/core/device/DeviceFingerprintProvider.kt` | DeviceFingerprintProvider, DeviceInfo |
| `app/src/main/java/com/trader/core/domain/model/CashBox.kt` | CashBox |
| `app/src/main/java/com/trader/core/domain/model/CashBoxMovement.kt` | CashBoxMovement, CashBoxMovementType, AdjustmentReason |
| `app/src/main/java/com/trader/core/domain/model/ChatMessage.kt` | MessageStatus, ChatMessage |
| `app/src/main/java/com/trader/core/domain/model/Customer.kt` | Customer |
| `app/src/main/java/com/trader/core/domain/model/DashboardAnalytics.kt` | TodaySalesSummary, TopSellingProduct, TopDebtorCustomer, DailySales, DashboardAnalytics |
| `app/src/main/java/com/trader/core/domain/model/Employee.kt` | EmployeeRole, Employee |
| `app/src/main/java/com/trader/core/domain/model/Inventory.kt` | InvoiceItem, InventorySession, InventorySessionItem, InventoryStatus |
| `app/src/main/java/com/trader/core/domain/model/Merchant.kt` | MerchantStatus, Merchant |
| `app/src/main/java/com/trader/core/domain/model/MerchantTier.kt` | MerchantTier |
| `app/src/main/java/com/trader/core/domain/model/PaymentMethod.kt` | PaymentMethod, PaymentType |
| `app/src/main/java/com/trader/core/domain/model/Product.kt` | Product, ProductUnit, StockLevel, ProductWithUnits, UnitType, WeightUnit, SyncStatus |
| `app/src/main/java/com/trader/core/domain/model/ReportAnalytics.kt` | FinancialReportTotals, DailySalesProfit, InventoryValue, DebtAging |
| `app/src/main/java/com/trader/core/domain/model/ReturnModels.kt` | TransactionReturnStatus, ReturnType, ReturnSyncStatus, ReturnInvoice, ReturnItem, ReturnSummary, ReturnUiState, Idle, Loading, Success, Error |
| `app/src/main/java/com/trader/core/domain/model/Session.kt` | Session |
| `app/src/main/java/com/trader/core/domain/model/StartupStatus.kt` | StartupStatus |
| `app/src/main/java/com/trader/core/domain/model/StockMovement.kt` | StockMovement, MovementType |
| `app/src/main/java/com/trader/core/domain/model/SubscriptionConfig.kt` | SubscriptionPlan, SubscriptionPaymentMethod |
| `app/src/main/java/com/trader/core/domain/model/SubscriptionRequest.kt` | SubscriptionRequest, SubscriptionStatus |
| `app/src/main/java/com/trader/core/domain/model/SubscriptionState.kt` | SubscriptionState |
| `app/src/main/java/com/trader/core/domain/model/Transaction.kt` | Transaction |
| `app/src/main/java/com/trader/core/domain/repository/ActivationRepository.kt` | ActivationRepository |
| `app/src/main/java/com/trader/core/domain/repository/AdminSubscriptionSettingsRepository.kt` | AdminSubscriptionSettingsRepository |
| `app/src/main/java/com/trader/core/domain/repository/CashBoxRepository.kt` | CashBoxRepository |
| `app/src/main/java/com/trader/core/domain/repository/ChatRepository.kt` | ChatRepository |
| `app/src/main/java/com/trader/core/domain/repository/CustomerRepository.kt` | CustomerRepository |
| `app/src/main/java/com/trader/core/domain/repository/DashboardAnalyticsRepository.kt` | DashboardAnalyticsRepository |
| `app/src/main/java/com/trader/core/domain/repository/EmployeeRepository.kt` | EmployeeRepository |
| `app/src/main/java/com/trader/core/domain/repository/InventoryRepository.kt` | InventoryRepository |
| `app/src/main/java/com/trader/core/domain/repository/InvoiceItemRepository.kt` | InvoiceItemRepository |
| `app/src/main/java/com/trader/core/domain/repository/LowStockAlertRepository.kt` | LowStockAlertCandidate, LowStockAlertRepository |
| `app/src/main/java/com/trader/core/domain/repository/LowStockSettingsRepository.kt` | LowStockAlertSettings, LowStockSettingsRepository |
| `app/src/main/java/com/trader/core/domain/repository/MerchantAdminRepository.kt` | MerchantAdminRepository |
| `app/src/main/java/com/trader/core/domain/repository/MerchantStatusRepository.kt` | MerchantStatusRepository |
| `app/src/main/java/com/trader/core/domain/repository/PaymentMethodRepository.kt` | PaymentMethodRepository |
| `app/src/main/java/com/trader/core/domain/repository/ProductRepository.kt` | ProductRepository |
| `app/src/main/java/com/trader/core/domain/repository/ReportsRepository.kt` | ReportsRepository |
| `app/src/main/java/com/trader/core/domain/repository/ReturnRepository.kt` | ReturnRepository |
| `app/src/main/java/com/trader/core/domain/repository/StockRepository.kt` | StockRepository |
| `app/src/main/java/com/trader/core/domain/repository/SubscriptionRepository.kt` | SubscriptionRepository |
| `app/src/main/java/com/trader/core/domain/repository/TransactionRepository.kt` | TransactionRepository |
| `app/src/main/java/com/trader/core/domain/usecase/GetDashboardAnalyticsUseCase.kt` | GetDashboardAnalyticsUseCase |
| `app/src/main/java/com/trader/core/sync/GlobalSyncState.kt` | GlobalSyncState, AllSynced, Pending, Failed, UnsyncedItem, UnsyncedItemType |
| `app/src/main/java/com/trader/core/sync/SyncCoordinator.kt` | SyncCoordinator |
| `app/src/main/java/com/trader/core/sync/SyncStatusObserver.kt` | SyncStatusObserver |
| `app/src/main/java/com/trader/core/util/DateUtils.kt` | DateUtils |
| `app/src/main/java/com/trader/core/util/ExpiryNotificationHelper.kt` | ExpiryNotificationHelper |
| `app/src/main/java/com/trader/core/util/ImageCompressor.kt` | ImageCompressor |
| `app/src/main/java/com/trader/core/util/NetworkMonitor.kt` | NetworkMonitor |
| `app/src/main/java/com/trader/core/worker/StatusCheckWorker.kt` | StatusCheckWorker |
| `salesmanager/src/main/java/com/trader/salesmanager/ChatNotificationService.kt` | ChatNotificationService |
| `salesmanager/src/main/java/com/trader/salesmanager/MainActivity.kt` | MainActivity |
| `salesmanager/src/main/java/com/trader/salesmanager/SalesManagerApp.kt` | SalesManagerApp |
| `salesmanager/src/main/java/com/trader/salesmanager/di/SalesManagerModule.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/service/NotificationService.kt` | NotificationService |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/activation/ActivationScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/activation/ActivationUiState.kt` | ActivationUiState |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/activation/ActivationViewModel.kt` | StartupState, Checking, Proceed, ProceedFree, NeedActivation, Blocked, ActivationViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/activation/MerchantWatcherViewModel.kt` | MerchantEvent, Disabled, Deleted, Expired, ExpiryWarning, MerchantWatcherViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/boxes/BoxesScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/boxes/BoxesViewModel.kt` | BoxesUiState, BoxesViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/chat/ChatScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/chat/ChatViewModel.kt` | ChatUiState, ChatViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/components/SharedComponents.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/customers/addedit/AddEditCustomerScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/customers/addedit/AddEditCustomerViewModel.kt` | AddEditCustomerUiState, AddEditCustomerViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/customers/details/CustomerDetailsScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/customers/details/CustomerDetailsViewModel.kt` | CustomerDetailsUiState, CustomerDetailsViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/customers/list/CustomersScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/customers/list/CustomersUiState.kt` | DeleteConfirmState, CustomersUiState |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/customers/list/CustomersViewModel.kt` | CustomersViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/debts/DebtsScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/debts/DebtsViewModel.kt` | CustomerDebt, DebtsUiState, DebtsViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/employees/EmployeeManagementScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/employees/EmployeeManagementViewModel.kt` | AddEmployeeError, AddEmployeeForm, EmployeeManagementEvent, EmployeeAdded, EmployeeDeleted, Failure, EmployeeManagementViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/home/AnalyticsDashboard.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/home/HomeScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/home/HomeUiState.kt` | DashboardUiState, HomeUiState |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/home/HomeViewModel.kt` | HomeViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/home/SyncStatusSheet.kt` | SheetHeaderData |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/home/SyncStatusViewModel.kt` | SyncStatusViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/inventory/addedit/AddEditProductScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/inventory/addedit/AddEditProductViewModel.kt` | UnitDraft, AddEditProductUiState, AddEditProductViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/inventory/detail/ProductDetailScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/inventory/detail/ProductDetailViewModel.kt` | ManualAdjustState, ProductDetailUiState, ProductDetailViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/inventory/invoice/InvoiceItemsScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/inventory/invoice/InvoiceItemsViewModel.kt` | InvoiceLineItem, InvoiceItemsUiState, InvoiceItemsViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/inventory/invoice/InvoiceUtils.kt` | SaleWeightUnit |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/inventory/list/InventoryListScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/inventory/list/InventoryListViewModel.kt` | InventoryListUiState, StockFilter, InventoryListViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/inventory/reports/StockReportsScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/inventory/reports/StockReportsViewModel.kt` | StockReportItem, CashSummary, StockReportsUiState, StockReportsViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/inventory/session/InventorySessionScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/inventory/session/InventorySessionViewModel.kt` | InventorySessionUiState, InventorySessionViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/navigation/AppNavigation.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/navigation/MainBottomNavigationBar.kt` | BottomNavItem |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/navigation/Screen.kt` | Screen, Activation, Home, CustomersList, AddCustomer, EditCustomer, CustomerDetails, TransactionsList, AddTransaction, EditTransaction, TransactionDetails, Reports, PaymentMethods, CashBoxes, Sessions, Debts, Settings, Backup, Chat, DayTransactions, Inventory, AddProduct, EditProduct, ProductDetail, InventorySession, InvoiceItems, StockReports, ReturnProcess, EmployeeManagement |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/navigation/SplashAndBlocked.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/payments/PaymentMethodsScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/payments/PaymentMethodsViewModel.kt` | PaymentMethodsUiState, PaymentMethodsViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/reports/DayTransactionsScreen.kt` | DayTxUiState, DayTransactionsViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/reports/ReportsScreen.kt` | ReportExportAction |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/reports/ReportsViewModel.kt` | ReportPeriod, DaySalesEntry, CustomerRank, PaymentShare, SalesProfitDayEntry, TimeOfDayAnalysis, ReportsUiState, ReportsViewModel, PeriodFinancials, AdvancedAnalyticsState, CalendarReportState |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/returns/ReturnProcessScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/returns/ReturnViewModel.kt` | ReturnLineState, ReturnScreenState, ReturnViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/scanner/BarcodeScannerScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/settings/SettingsScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/settings/backup/BackupCrypto.kt` | BackupCrypto, FileHeader |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/settings/backup/BackupFormatting.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/settings/backup/BackupManager.kt` | BackupManager, GsonHolder, BackupException, NewerBackupVersion, UnsupportedBackupVersion, Corrupted, WrongPasswordOrCorrupted, PasswordRequired, EmptyBackup, InvalidFile, StorageFailed, ReadFailed |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/settings/backup/BackupMigrator.kt` | BackupMigrator |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/settings/backup/BackupPayload.kt` | BackupPayload |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/settings/backup/BackupScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/settings/backup/BackupViewModel.kt` | BackupUiState, BackupViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/settings/sessions/SessionsScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/settings/sessions/SessionsViewModel.kt` | SessionsViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/theme/Color.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/theme/Theme.kt` | AppColorTokens |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/theme/Type.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/transactions/addedit/AddEditTransactionScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/transactions/addedit/AddEditTransactionViewModel.kt` | AddEditTransactionUiState, AddEditTransactionViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/transactions/details/TransactionDetailsScreen.kt` | PdfExportAction |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/transactions/details/TransactionDetailsViewModel.kt` | TransactionDetailsUiState, TransactionDetailsViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/transactions/list/TransactionsScreen.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/transactions/list/TransactionsUiState.kt` | TransactionsUiState |
| `salesmanager/src/main/java/com/trader/salesmanager/ui/transactions/list/TransactionsViewModel.kt` | TransactionsViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/update/AppUpdateChecker.kt` | AppUpdateChecker |
| `salesmanager/src/main/java/com/trader/salesmanager/update/AppUpdateDownloader.kt` | AppUpdateDownloader |
| `salesmanager/src/main/java/com/trader/salesmanager/update/AppUpdateInfo.kt` | AppUpdateInfo |
| `salesmanager/src/main/java/com/trader/salesmanager/update/AppUpdateViewModel.kt` | AppUpdateUiState, AppUpdateViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/update/BackgroundUpdateWorker.kt` | BackgroundUpdateWorker |
| `salesmanager/src/main/java/com/trader/salesmanager/update/UpdateDownloadReceiver.kt` | UpdateDownloadReceiver |
| `salesmanager/src/main/java/com/trader/salesmanager/util/InvoiceSharer.kt` | InvoiceSharer |
| `salesmanager/src/main/java/com/trader/salesmanager/util/export/ExportActionButton.kt` |  |
| `salesmanager/src/main/java/com/trader/salesmanager/util/export/ExportManager.kt` | ExportManager |
| `salesmanager/src/main/java/com/trader/salesmanager/util/export/ExportState.kt` | ExportState, Idle, Loading, Success, Error, ExportType, ExportTarget |
| `salesmanager/src/main/java/com/trader/salesmanager/util/export/ExportViewModel.kt` | ExportViewModel |
| `salesmanager/src/main/java/com/trader/salesmanager/util/export/XlsxWriter.kt` | XlsxWriter, CellStyle |
| `salesmanager/src/main/java/com/trader/salesmanager/util/pdf/InvoicePdfGenerator.kt` | InvoicePdfGenerator, InvoicePdfData |
| `salesmanager/src/main/java/com/trader/salesmanager/util/pdf/PdfCanvas.kt` | PdfCanvas |
| `salesmanager/src/main/java/com/trader/salesmanager/util/pdf/PdfLayoutConstants.kt` | PdfLayoutConstants |
| `salesmanager/src/main/java/com/trader/salesmanager/util/pdf/PdfPrintManager.kt` | PdfPrintManager, ExistingPdfAdapter |
| `salesmanager/src/main/java/com/trader/salesmanager/util/pdf/ReportPdfGenerator.kt` | ReportPdfGenerator, MonthlyReportPdfData, DebtRow |
| `salesmanager/src/main/java/com/trader/salesmanager/worker/LowStockCheckWorker.kt` | LowStockCheckWorker |
| `salesmanager/src/main/java/com/trader/salesmanager/worker/UnpaidDebtWorker.kt` | UnpaidDebtWorker |

Total production Kotlin files enumerated: **200**; declared types found by source declaration scan: **340** (includes nested types, not a bytecode class count). No production Java files found. The standalone qa/pdf-verifier contains test-only Java,not application production code. Enumeration is complete; deep method-by-method review remains incomplete.

## Continuation architecture
Room version23 persists originalAmount/returnStatus. ReturnRepository now has durable syncPendingReturns,atomic remote merge guards,and stable-ID replay checks;SyncCoordinator/Seller DI wire retries. QA adds an isolated Android runner and fake customer journeys plus native PDF generation. The independent Java/PDFBox host verifier consumes only real device-produced PDFs. No production classes were excluded from coverage. See seller-audit-continuation.md for tested stages and remaining scope.

Stage8 traced AddEditTransactionViewModel→TransactionRepository→cash, then separate invoice/stock writes, and physical deletion followed by independent network work. Debt is derived from unpaid transactions, not a separate creation table. Durable transactional outbox is absent. See stage8-atomicity-and-outbox.md for verified boundaries and unimplemented guarantees.


## Coverage target 40% acceptance (2026-11-09)

Both requested line targets now pass: Seller **7668/17795 =43.0908%**; weighted shared+Seller **11525/28062 =41.0698%** (summed counters, not a mean of percentages). Branches: Seller3703/14316 =25.8662%, weighted4346/17333 =25.0736%. Starting source dff4876 had221 JVM tests, Seller1123/17794 =6.3111%, weighted4980/28061 =17.7471%. This stage adds35 JVM behavioral tests:27 offline Compose journeys,4 ReportsViewModel calculations,4 actual XLSX/OpenXML export checks. Complete256 JVM tests pass (shared95/Seller161),0 failures/errors/skips;5 separate Python coverage-gate tests also pass.

Four XLSX content regressions failed before the repair:shared strings were serialized before registration, duplicate fills inflated their count, and solid-fill references used a reserved index. XlsxWriter now serializes the populated string table and consistent fill references. Actual ZIP/XML data, Arabic/XML escaping, literal formulas, totals and stock values are checked. No financial rules, Admin business logic, schema or coverage exclusions changed. Seller LINE floor is raised3→40%; weighted LINE40% is now enforced independently. Existing shared and branch floors remain unchanged.

Final combined JVM/JaCoCo/Lint/Debug/AndroidTest/minified unsigned Release Gradle run succeeds; Lint0 errors/24 warnings. Release signature verification rejects the APK as expected (unsigned; no production signing material). Targeted secret policy and diff whitespace checks pass. Test application uses Config.NONE/plain Application; no production Firebase service is contacted. Coverage is JVM debug coverage, not device/release runtime coverage. See coverage40-progress.md and evidence/continuation/coverage40/final/ for actual counters, build logs, APK hashes and lint XML; XLSX before/after evidence is under coverage40/xlsx/.

Hosted milestone run42583691086:Compose SUCCESS; Seller FAILED in Install verified Gradle, before tests (later Seller test/build steps skipped). This is not a passing hosted test claim. Final-source hosted verification is pending. No raw hosted XML/PDF artifacts have been independently downloaded. Server authorization/identity rules, partial-backend reconciliation, legacy tenant isolation and authentic historical migration/real upgrade evidence remain release blockers. Release readiness remains **FAIL**, independently of achieving both coverage targets.


## Final hosted coverage acceptance

Seller QA run42586688384 at source **47148214d22f15097f30e8b7374e735648a597f6** completed **SUCCESS**:both **seller SUCCESS** and **compose SUCCESS**. URL:https://github.com/mohammedakkad/SalesManager/actions/runs/42586688384 . All14 Seller job steps and11 Compose job steps show success, including hosted tests/coverage/40% floors/Lint/Debug, minified unsigned Release, emulator/device/PDF verification and artifact upload. This supersedes final-source pending status above. The device script fails unless all4 named instrumentation cases execute/pass in XML, both Android-generated PDFs are nonempty and the2 real PDFBox content tests pass; the hosted script step succeeded. Raw hosted XML/PDF files and hosted numerical coverage counters were not independently downloaded. Local exact256 JVM/5 Python results and line numerators/denominators remain separately recorded.

Evidence:evidence/continuation/coverage40/final/hosted-success.json plus jvm-cases.json and counters/build logs. This verifies source4714821; the following documentation-only commit is not claimed as another completed CI run. Updating PR51 body via gh was blocked by Forbidden at api.github.com/graphql; its proposed current body is saved as draft-pr-body.md, and branch commits are pushed to the existing PR. No merge or new PR. Both requested40% line targets are achieved without exclusions; overall release readiness remainsFAIL for the previously documented server/tenant/distributed/upgrade blockers.


## Coverage 70% verified increment (2026-11-09)

Both requested line targets achieved:Seller **12575/17795 =70.6659%**, weighted shared+Seller **19693/28062 =70.1768%**. Shared7118/10267 =69.3289%. Branches:Seller6314/14318 =44.0983%; weighted7346/17333 =42.3816%. Complete355 JVM tests PASS (shared125/Seller230),0 failures/errors/skips, plus5 separate Python gate tests PASS. This adds99 JVM tests from starting10f8064:36 offline Seller UI journeys,33 Seller command/worker cases and30 real SQLite query/repository contracts. No coverage exclusions or weakened floors; both Seller and weighted LINE floors rise40→70%; existing shared and branch floors remain unchanged.

Two actual regressions fail before and pass after repair:unit deletion now captures its parent product through getProductIdForUnit before local removal, preserving the remote delete arguments; return classification now requires every remaining returnable line to be selected before labeling the command FULL. Refund formulas and Room transaction boundaries are unchanged. No Admin business files/schema changes, production data/signing material, or destructive migrations. ProductRepositoryImpl is the required shared Seller dependency.

Full test/JaCoCo run succeeds in1m41s. Final combined JVM/coverage/Lint/Debug/AndroidTest/minified unsigned Release acceptance succeeds in4m22s; Lint0 errors/24 warnings. Unsigned Release is deliberately rejected by apksigner verification; no distribution/signing claim. Targeted tracked secret policy and whitespace checks PASS. Coverage is actual debug JVM coverage, not physical-device/release coverage. Central report:coverage70-progress.md; evidence:evidence/continuation/coverage70/final/ includes counters,355 actual JUnit cases, gates, lint XML, build logs and APK hashes. Before/after fault evidence:coverage70/unit-deletion/ and coverage70/return-selection/.

Milestone hosted run42624578824 at c4c1e65 is SUCCESS, both seller/compose SUCCESS; final70% source hosted acceptance remains pending. Raw hosted XML/PDF files have not been independently downloaded. Release readiness remains **FAIL** for documented server authorization/identity, distributed reconciliation, legacy tenant/manual-write isolation and historical schema/upgrade limits. These tests additionally expose the legacy stock-ledger retry gap:getPending selects only PENDING, excluding FAILED; its successful legacy retry fixture explicitly starts a new PENDING attempt. This is not a claim that FAILED manual stock mutations automatically retry. Catalog unit deletion remains best effort and lacks a durable deletion tombstone; the parent-ID repair does not supply an outbox. Managed sale outbox guarantees remain separately documented.


## Final hosted 70% acceptance

Seller QA run **42628091348** at audited source **7df9409760f7ea59ca449e7774736a7d0d26effd** completed **SUCCESS**, with **seller SUCCESS** and **compose SUCCESS**:https://github.com/mohammedakkad/SalesManager/actions/runs/42628091348 . All14 Seller and11 Compose public steps show success, including tests/JaCoCo/both70% line gates/Lint/Debug, minified unsigned Release, device/PDF pipeline and artifact uploads. This supersedes pending final-source status above. The device script is fail-closed:all4 named instrumentation tests must execute/pass in actual XML, both Android-generated PDFs must be nonempty, and the2 PDFBox content tests must pass. Its hosted step succeeded. Raw hosted XML/PDF files and numerical coverage counters have not been independently downloaded; local exact355 JVM+5 Python results and counters are separately preserved.

Sanitized metadata:evidence/continuation/coverage70/final/hosted-success.json. Both requested70% line targets are achieved and enforced in hosted CI without exclusions. The subsequent evidence-only documentation commit is not claimed to have separately completed CI. Branch commits update existing draft PR51; direct PR-body editing remains unavailable through the policy-blocked GitHub API, so draft-pr-body.md contains the current reviewable description. No merge/new PR. Overall release readiness remainsFAIL for the documented server, tenant, distributed, legacy retry/tombstone and authentic historical migration/upgrade limitations.


## Toward 100% — verified phase1

Source starts94a2e48. Complete382 JVM cases PASS (shared135/Seller247),0 failures/errors/skips:27 additional cases; all355 prior qualified regressions retained (one stock-retry test strengthened/renamed). Seller lines12937/17810 =72.6390%; weighted20531/28079 =73.1187%; weighted branches7742/17359 =44.5993%. Eleven separate Python coverage guards PASS. Existing70% JVM floors and the production inclusion policy remain unchanged; measured denominators reflect the repaired source. Literal100% is NOT achieved. Historical1–21 fixture gaps, Admin-only code outside Seller scope and nonbehavioral generated paths prevent an honest claim of100% under the current constraints.

Five regressions fail before repair and pass afterward:FAILED stock ledger retry now selects persisted FAILED rows without resetting/replaying stock; the open debt screen reacts to transaction changes using the same derived SQLite debt query; Firebase upload/fetch/listeners preserve paymentType with backward-compatible DEBT fallback; truncated downloads are rejected/removed; cancellation propagates and closes/removes unfinished downloads. SDK fakes/loopback HTTP do not prove deployed server authorization or APK authenticity. Cancellation is cooperative; blocking socket reads still use bounded timeouts. No Admin business files, schema versions, or financial formulas changed.

Local complete test/coverage/Lint/Debug/AndroidTest/minified unsigned Release acceptance PASS (5m24s); Lint0 errors/24 warnings, targeted secret policy PASS. PDFBox4 cases compile locally but native PDF device runtime is pending. Two native bitmap-helper tests pass; no JVM PDF shadow/fake PDF was added. Device suite expands4→6 and PDFBox2→4; opt-in Android execution coverage and a separate combined report preserve the original JVM metric. Missing device data and missing required XML correctly fail controls. Counter/class identity mismatches reject the combined claim. Named runtime outcomes and measured coverage are published to the hosted job summary; actual hosted phase1 execution remains pending.

Actual inventory:evidence/continuation/coverage100/final/test-inventory.json; remaining-test-plan.csv defines73 remaining work items (41 NOT TESTED,32 BLOCKED), not73 exhaustive unit tests. Its21 historical migration paths are blocked on authentic fixtures. Official emulator binary access was probed and denied403 by the current network policy; no production Firebase was contacted. Local MainActivity lifecycle diagnostics stalled and were removed from the qualified suite, not counted as passing/skipped; device qualification remains pending. Evidence:coverage100/phase1/,stock-retry/,debt-refresh/,payment-type/,download/,environment/. Detailed final accounting follows hosted execution and the next financial-model checks. Release readiness remainsFAIL.


## Toward100% — additional boundary qualification

Current local source:394 JVM cases PASS (shared135/Seller259),0 failures/errors/skips;39 net new cases since94a2e48. Seller lines13158/17813=73.8674%; weighted20752/28082=73.8979%; weighted branches7849/17365=45.2001%. Eleven Python coverage guard tests PASS. The original JVM70% gates and inclusion policy remain unchanged. Literal100% is NOT achieved;4655 Seller lines and7330 weighted lines are still missed by JVM execution.

New failing-before/passing-after regressions repair the late invoice-load preview (displayed150/committed160 now both160), daily cash end boundary (half-open local day includes23:59:59.999), and duplicate product-save submissions (2 writes now1; explicit failure allows a controlled retry). Six invoice-editor contracts, two product-submit contracts and one day-boundary case pass. Three additional actual Room/WorkManager reminder cases verify customer totals/navigation, recent/paid exclusions and no debt mutation. No schema or financial formula changed.

Full JVM/JaCoCo/gates/Lint/Debug/AndroidTest/minified unsigned Release acceptance PASS; final test-only suite and updated AndroidTest APK PASS. Lint0 errors/24 warnings; targeted secret policy PASS. Authentic schemas remain22–25, current Room25; historical1–21 remainBLOCKED. Before/after and final evidence:coverage100/editor/,day-boundary/,product-submit/,boundaries-final/.

Hosted run42640961313 atb7b8d33:SellerSUCCESS, ComposeFAILURE (emulator action), artifact uploadSUCCESS. Exact failure/device XML was unavailable from public logs; no individual device/PDF success is claimed. Public actual-XML and measured-counter annotations plus stage/exit diagnostics were added for the next run, alongside retained artifacts and summary reports. Statement fixture now uses fractional20.25/30.50 with strict total50.75 assertions; integer monetary formatting was never a fixed-two-decimal contract. Original invoice assertions remain unchanged. New full hosted source verification is pending.

Remaining defined plan:70 work items (38NOT TESTED/32BLOCKED), after closingLOG-01/02/06 with actual regressions. These are work items, not an exhaustive finite count of all possible tests. See remaining-test-plan.csv and evidence/continuation/coverage100/final/test-inventory.json. Server authorization/fencing, partial Firestore/RTDB reconciliation, legacy tenant/cache ownership, cross-device identities, historical fixtures and signed upgrades remain release blockers. Release readinessFAIL.


## Final local JVM qualification toward100%

400 JVM tests PASS (shared135/Seller265),0 failures/errors/skips;45 net additional cases since94a2e48. Six further invoice-details contracts cover live derived debt, walk-in/missing transactions, sorted/stable invoice items, live/explicit return summaries, awaited aggregate deletion and cancellation without a success flag. Full JVM/JaCoCo/gates and Lint PASS; Lint0 errors/24 warnings. Seller lines13228/17813=74.2604%; weighted20822/28082=74.1471%; weighted branches7893/17365=45.4535%.4585 Seller and7260 weighted lines remain missed. Current JVM target100% NOT achieved; original70% floors and production inclusion policy remain unchanged. The earlier successful full Debug/AndroidTest/minified unsigned Release build covers identical production code; final increment changes tests/docs only.

An isolated official Robolectric4.16+AndroidX/native-graphics API28 runtime probe also fails at actual PdfDocument.startPage with "document is closed!". This diagnostic is explicitly FAILED, outside the passing400-case suite; no custom PDF shadow/fake bytes or root dependency upgrade was added. Evidence:coverage100/pdf-runtime-416-probe/ and details-final/. True Android PDF runtime and additional Android+JVM counters must be read separately from the original JVM metric. Hosted source verification remains in progress, not claimed successful here. Remaining plan70 defined work items (38NOT TESTED,32BLOCKED), not all possible tests; release readinessFAIL.


## Export lifecycle qualification and genuine return-PDF extension

Current local acceptance:409 JVM cases PASS (shared135/Seller274),0 failures/errors/skips;54 net new cases since94a2e48. Seller lines13291/17824=74.5680%; weighted20885/28093=74.3424%; weighted branches7918/17373=45.5765%.4533 Seller/7208 weighted lines remain missed. Unchanged70% JVM gates and production inclusion policy PASS; target100% NOT achieved. Complete JVM/JaCoCo/Lint/Debug/AndroidTest/minified unsigned Release PASS in5m14s; Lint0 errors/24 warnings. Eleven Python guards PASS, targeted secret policy PASS, release signature verification rejects the expected unsigned APK. No Admin/schema/financial-rule changes.

Three actual export regressions fail before repair and pass afterward:synchronous Loading prevents duplicate enqueue; cancellation/timeout is not converted to Error; reset cancels and invalidates previous work so late success cannot resurrect. Nine lifecycle tests include real XLSX success, explicit retry, IO/OOM error mapping and public PDF entry-point failure contracts. Synchronous IO generators may still finish cache writes before observing cancellation; no immediate interruption/deletion guarantee is claimed. Evidence:coverage100/export-lifecycle/.

Hosted400-test sourcee1771fe is SUCCESS:https://github.com/mohammedakkad/SalesManager/actions/runs/42690764128 . Both jobs succeed; actual XML annotations show6 instrumentation/4 PDFBox PASS,0 failure/skip. Measured additional Android+JVM Seller14189/17813=79.6553%, weighted21801/28082=77.6334%; these source-specific metrics do not replace the original JVM report. Preserved public evidence:coverage100/hosted/run-42690764128.json. Raw authenticated artifacts were not independently downloaded.

Next hosted runtime requirement expands to7 instrumentation/5 PDFBox:ExportViewModel must generate a genuine partial-return invoice on Android, and PDFBox must verify identity, gross100/refund25/net75, derived balances10/85 and a currency deduction. Existing content assertions remain intact. Actual Context paths and SHA256 values are now exported from device logs to public annotations; no package path is inferred. This new runtime is PENDING, not passed by compilation. Defined remaining plan69 items (37NOT TESTED/32BLOCKED); LOG-04 is closed by actual regressions. Detailed Arabic accounting:test-completion-status-ar.md. Room25 and historical1–21/server/tenant/distributed/security/upgrade blockers remain; release readinessFAIL.


## Final hosted qualification toward100% — audited sourceef1c899

Workflow https://github.com/mohammedakkad/SalesManager/actions/runs/42693672936 :SUCCESS. Both seller and composeSUCCESS, including test/Lint/build/release and artifact-upload steps. Actual XML annotations prove7 instrumentation and5 PDFBox casesPASS,0 failures/errors/skips. Five actual Context paths under com.trader.salesmanager.debug and their SHA256 values are preserved, with shell exports verified before app cleanup. The ViewModel partial-return invoice passes identity/gross/refund/net/derived-balance/deduction checks. Raw authenticated PDFs/XML were not independently downloaded; public runtime evidence is retained incoverage100/hosted/run-42693672936.json.

Original JVM metric:409 app/Seller casesPASS (shared135/Seller274),11 Python guardsPASS, Seller13291/17824=74.5680%, weighted20885/28093=74.3424%; weighted branches7918/17373=45.5765%. Separate actual Android+JVM metric at the same source:Seller14295/17824=80.2009%, weighted21906/28093=77.9767%. No lowered gates, exclusions, fake PDFs or exactly-once claims. Target100% remainsNOT ACHIEVED. Lint0 errors/24 warnings; Debug/AndroidTest/minified unsigned ReleasePASS. Room library2.6.1/schema25; authentic22–25 fixtures retained,1–21BLOCKED.

A preselected eight-fault sample in a disposable git archive is detected by real Room regressions (baseline8PASS, eight mutantsKILLED, ten intentional failed-case executions,0surviving/invalid mutants). Main production source and all measured coverage counters stayed byte-for-byte/number-for-number unchanged; these are not new production cases or a comprehensive mutation score. SaleCommands mutation qualification isPARTIAL. Evidence:coverage100/sale-mutation-sample/.

Since94a2e48:54 new app/Seller JVM,6 Python,3 Android and3 PDFBox cases;432 distinct currently passing qualification cases across those separate suites, excluding repeated/mutant/failed diagnostic executions. Remaining explicit plan69 work items:36NOT TESTED,32BLOCKED,1PARTIAL; not an exhaustive count of all possible tests. Existing server fencing/authorization, Firestore/RTDB reconciliation, legacy tenant/cache/cross-device identity, historical fixtures, signed upgrade/security/performance/API/lifecycle blockers remain. Release readinessFAIL. Detailed Arabic accounting:test-completion-status-ar.md; structured qualification:coverage100/final/qualification.json. This final evidence increment changes documentation only after the audited source.

## Previous verified boundary continuation — 2026-11-10

Starting SHA: `78bc60543d490cf47317afb832f3d5bf1df6d43e`. Qualified source: `53ba5722ffe485f1ebcda55316e96830ac849b3b`, on the existing branch and PR #51. All **447 module JVM cases pass** (shared 135, Seller 312), with zero failures, errors or skips. All 409 starting identities remain; 38 cases were added. Eleven Python coverage guards pass. Seller JVM lines: **13440/17862 = 75.2435%**; weighted lines: **21034/28131 = 74.7716%**. Branches: Seller 6689/14446 = 46.3035%, weighted 8066/17473 = 46.1627%. The original 70% gates and production inclusion policy remain unchanged. The 100% target is **NOT ACHIEVED**.

Nineteen actual failing-before regression conditions led to repairs for barcode debounce and stale callbacks, nonfinite product inputs, preservation of a typed zero threshold, deletion of persisted units only, session revoke ordering/retry/UI, and complete invoice restoration with cancellation and Save guarding. Six additional invoice numeric cases cover NaN and overflow while preserving the permitted zero-price behavior. No Admin business logic, financial formulas, schema or server protocol changed.

[Hosted run 42762481171](https://github.com/mohammedakkad/SalesManager/actions/runs/42762481171) is **SUCCESS**: `seller` and all four `compose` lanes (API 24, 28, 33, 35) succeed. Each lane has seven named Android cases and five real PDFBox cases passing, five actual Context/output paths with SHA256 hashes, measured EC coverage and a preserved artifact. These are 28 Android and 20 PDFBox executions of the same seven and five unique implementations. The separate API 28 Android+JVM measurement is Seller **14444/17862 = 80.8644%**, weighted **22055/28131 = 78.4011%**; it does not replace JVM gate counters.

API 24 failed during APK installation in attempts 42757328279 and 42759200717. Attempt 42761236105 installed and reached instrumentation, then exposed a shell-export failure with an empty hash result and UiAutomation permission/connection errors. The repair reads the actual exported file as binary, compares every byte and computes SHA256 with Android Java instead of depending on a shell hash utility. The subsequent complete API 24 runtime passes. Failed attempts remain documented; no PDF content assertion or coverage instrumentation was removed.

Local complete JaCoCo/gates/Lint/Debug/AndroidTest/minified unsigned Release acceptance passed in 5m9s on production source 6256d32, which remains unchanged. The final 447-case numeric JVM/coverage/Lint acceptance passed in 1m58s; the final hosted workflow also rebuilds and executes its full checks. Lint: zero errors, 24 warnings. Targeted secret policy passes; it is not a complete security audit. Room 2.6.1/schema 25, authentic schemas 22–25 and seven migration cases remain intact; historical versions 1–21 remain BLOCKED. No destructive migration or production signing was used.

The explicit remaining plan has **63 work items: 29 NOT TESTED, 32 BLOCKED, two PARTIAL**. This is not a count of all possible tests. P0/P1 server authorization/revision fencing, cross-backend reconciliation, legacy tenant/cache/writer ownership, authentic historical fixtures, signed upgrades, and broader security/performance/device qualification remain unresolved. Release readiness is **FAIL**. See [Arabic accounting](test-completion-status-ar.md), [current structured evidence](evidence/continuation/coverage-next/final/qualification.json) and [actual per-API results](evidence/continuation/coverage-next/matrix/run-42762481171.json).

## Current recovery continuation — 2026-11-10

Starting SHA: `b9c978f6bb9e6a3d00eb895a386c7f856aae995c`. Qualified source: `59413ed14a306df0fff2d7127cc2ad0c90217971`, on the same branch and PR #51. **514 JVM cases PASS** (shared 162/Seller 352), zero failures/errors/skips; all 447 starting identities retained, **67 new cases**. Eleven Python coverage guards pass. Seller JVM lines **13619/17895 = 76.1051%**, weighted **21278/28192 = 75.4753%**; branches Seller6868/14660 = 46.8486%, weighted8312/17763 = 46.7939%. Original gates/inclusions remain unchanged. The 100% target is NOT ACHIEVED.

Actual pre-repair evidence contains43 distinct failing regression cases (44 executions; one Room case repeated). A lost result after a committed partial return previously allowed a second refund: sale25→0, cash125→100, stock3→4 and two RETURN events. Stable unchanged retry identities now keep the one committed refund. Submission claims synchronously; active drafts/modal controls cannot reopen a second request; completed results cannot resubmit. Failed reads no longer invent zero prior returns; finite/overflow guards, visible errors, explicit reload and cancellation handling preserve existing zero-price/deleted-product behavior. Client retry identity remains in memory; actual edits create new intent, and OS process-restoration is not certified.

All eight Seller SDK listener families now close/unregister on terminal errors without fabricated empty snapshots. Consumers recover inside merchant boundaries: at most three subscriptions with 500/1000ms delays for transient errors; denied credentials are not retried. Cancellation/programming errors propagate. Payment and employee logout now cancels the old listener. Exhausted-stream UI status/same-session retry and remote snapshot numeric quarantine remain explicit unfinished work. New cash guards reject nonfinite input and roll back SQL arithmetic overflow inside Room before ledger commit, preserving existing finite negative-balance rules and monetary formulas. Three isolated production Koin contracts verify shared DAO database identity, actual atomic sale persistence and Return navigation parameters; not MainActivity runtime.

[Hosted run 42836384853](https://github.com/mohammedakkad/SalesManager/actions/runs/42836384853) is **SUCCESS**: seller and compose on API24/28/33/35. Each lane executes all seven Android and five real PDFBox cases, records five actual Context/export paths and hashes, measures EC coverage and preserves artifacts. Separate API28 Android+JVM lines: Seller **14624/17895 = 81.7212%**, weighted **22300/28192 = 79.1005%**; JVM gates retain their own counters. No fake PDF or exclusion was introduced.

Full local JVM/Lint/Debug/AndroidTest/minified unsigned Release aggregate PASS in 5m32s, followed by final514 JVM/JaCoCo PASS in 1m37s and current Lint PASS in 37s. A prior asynchronous Room/Compose failure assertion was synchronized with actual completion; the original displayed-error and rollback/retry assertions remain. Lint: 0 errors/24 warnings; targeted secret policy and11 Python guards PASS. Release is unsigned QA output, not signed device-upgrade qualification. Room 2.6.1/schema 25/authentic22–25 and seven migration cases remain; no new DDL, destructive fallback, Admin business logic, production service access or signing material.

The explicit remaining plan has **56 work items: 23 NOT TESTED, 32 BLOCKED, one PARTIAL**. Nine client/input items close; two discovered follow-ups are added openly. P0/P1 deployed authorization/revision fencing, distributed reconciliation, legacy tenant/cache/writer ownership, authentic historical1–21 fixtures, signed upgrades, and broader security/device/performance work remain. Release readiness **FAIL**. [Arabic details](test-completion-status-ar.md), [structured qualification](evidence/continuation/coverage-recovery/final/qualification.json), [actual per-API runtime](evidence/continuation/coverage-recovery/hosted/run-42836384853.json).

## Verified local 95% continuation — 2026-11-10

Current 95% continuation (2026-11-10): 593 JVM PASS, 79 new; Seller JVM lines 77.6573%, weighted 78.1178%. Target 95% NOT ACHIEVED; release FAIL. See [current evidence](coverage95-progress.md). Earlier counters below are historical.

|Metric (JVM)|Seller covered/total|Coverage|Weighted covered/total|Coverage|
|---|---:|---:|---:|---:|
|LINE|13896/17894|77.6573%|22030/28201|78.1178%|
|METHOD|2390/3200|74.6875%|4208/5715|73.6308%|
|BRANCH|7151/14696|48.6595%|8766/17807|49.2278%|

Full JVM/Room/JaCoCo/gates, Lint, Debug/AndroidTest APK and minified unsigned Release builds PASS. Lint 0 errors/24 warnings; 12 Python coverage guards and targeted secret/token-log policy PASS. All 514 starting JVM identities retained; 208 shared and 385 Seller cases, zero failures/errors/skips. Actual source protocol fakes do not qualify deployed Firebase rules, deduplication or distributed rollback. Hosted source verification is pending for this commit; the prior successful run is not reassigned to it. No Admin business, database schema, production exclusions, gates or signing material changes. [Details and exact evidence](coverage95-progress.md).

## Final local download follow-up — 2026-11-10

|Metric (local JVM)|Seller covered/total|Coverage|Weighted covered/total|Coverage|
|---|---:|---:|---:|---:|
|LINE|13921/17898|77.7796%|22055/28205|78.1954%|
|METHOD|2391/3200|74.7188%|4209/5715|73.6483%|
|BRANCH|7148/14684|48.6788%|8763/17795|49.2442%|

Source `fda922fb865dabf5633115268d3b4e72ccbb8e4f`:601 JVM PASS (208 shared/393 Seller, zero failures/errors/skips);87 new cases and all514 starting identities retained. Full current JVM/Room/JaCoCo/gates/Lint/Debug/AndroidTest/minified unsigned Release aggregate PASS in5m36s; Lint0 errors/24 warnings;12 Python guards and targeted secret policy PASS. Five additional failing-before resolver regressions now pass; incomplete downloads return false and attempt pending-row cleanup. Real XLSX source bytes are verified on SDK-fake API33 and real legacy filesystem API28. This does not qualify device MediaStore permission/provider behavior. No Admin, schema, threshold, exclusion, production network or signing changes. [Current hosted status and detailed results](coverage95-progress.md).

Hosted source qualification: [42863854908](https://github.com/mohammedakkad/SalesManager/actions/runs/42863854908) SUCCESS (seller and compose API24/28/33/35);7Android and5PDFBox cases pass per lane with5path/hash records and preserved artifacts. Clean JVM denominators reconcile with hosted compilation; clean601casesPASS. Additional Android+JVM line coverage83.3948% Seller/81.8188% weighted; target95% NOT ACHIEVED and releaseFAIL. See coverage95-progress.md for all Line/Method/Branch counters and their exact denominators.

Final clean packaging PASS in4m44s after the clean601JVM/JaCoCo run (2m34s): Lint0errors/24warnings, Debug APK, AndroidTest APK and actual R8 unsigned Release. Current SHA256/size records and preserved local APK paths are in final/qualification.json. The hosted source remains fda922fb865dabf5633115268d3b4e72ccbb8e4f; the final documentation-only commit does not change production/test sources. No PR merge or PR-description publication is claimed.

## Verified continuation — 2026-11-11

Source `ee1fcc2ecf41ebba24884e366d6f8abf9748c467`; starting HEAD `68e99a76286faada1fcaedfba41749785789d7bb`, same branch/PR51.632JVM PASS (208shared/424Seller),31new and all601starting identities retained; zero failures/errors/skips.22Python guards PASS (10new). Ten actual failing-before UI regressions pass after repair; fixture-only failures are excluded from that count.

|Measured scope|Line covered/total|Method covered/total|Branch covered/total|
|---|---:|---:|---:|
|Seller — project-local JVM|14223/17903 = 79.4448%|2463/3201 = 76.9447%|7402/14708 = 50.3264%|
|Weighted — project-local JVM|22357/28210 = 79.2520%|4281/5716 = 74.8950%|9017/17819 = 50.6033%|
|Weighted — both JVM suites|22671/28210 = 80.3651%|4399/5716 = 76.9594%|9127/17819 = 51.2206%|
|Seller — Android + JVM|15228/17903 = 85.0584%|2533/3201 = 79.1315%|7567/14708 = 51.4482%|
|Weighted — Android + module JVM|23379/28210 = 82.8749%|4369/5716 = 76.4346%|9182/17819 = 51.5293%|
|Weighted — Android + both JVM suites|23680/28210 = 83.9419%|4473/5716 = 78.2540%|9292/17819 = 52.1466%|

Full clean local aggregate PASS in6m40s, including original coverage gates, both-JVM reports, Lint, Debug/AndroidTest APKs and actual R8 minified unsigned Release. Lint0errors/24warnings; targeted credential/signing/token-log policyPASS. [Hosted run42983779778](https://github.com/mohammedakkad/SalesManager/actions/runs/42983779778): seller and all four compose lanesSUCCESS;7Android and5PDFBox cases per lane with five actual PDF paths/hashes and preserved artifacts. Public XML-derived notices and EC-counter records are preserved; raw authenticated archives/checkout are not independently downloaded.

Whole-suite reports add actual Seller-test execution of shared app code. Original project-local reports/gates, production class lists and denominators remain unchanged; the additional scope is not silently substituted for the historical baseline or attributed entirely to the31new tests. UI save failures remain retryable, filtered inventory searches no longer invent completion, and action accessibility improves. Existing monetary/stock/debt formulas and DB schema remain unchanged; no Admin business changes or production data/service access. Room2.6.1/schema25 and seven migration cases remain; authentic historical1–21fixtures are stillBLOCKED.56remaining qualification items (22NOT TESTED/32BLOCKED/2PARTIAL), server authorization/fencing, distributed reconciliation, legacy tenant/writer ownership and broader device/security/performance work remain open. ReleaseFAIL.
