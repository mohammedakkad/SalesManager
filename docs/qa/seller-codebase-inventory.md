# Seller codebase inventory — Stage A

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
