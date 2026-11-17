"""Named runtime requirements; shared by XML validation and public evidence summaries."""
INSTRUMENTATION = {
    ('com.trader.salesmanager.qa.ThemedDialogJourneyTest', 'encryptedExportAfterThemeChangeProducesAnArchiveDecryptableWithTheOriginalPassword'),
    ('com.trader.salesmanager.qa.ThemedDialogJourneyTest', 'exportDialogKeepsBothPasswordsAcrossThemeChangeAndCancelDoesNotExport'),
    ('com.trader.salesmanager.qa.ThemedDialogJourneyTest', 'inventoryCountDraftSurvivesThemeRoundTripAndOnlyConfirmationWritesRoom'),
    ('com.trader.salesmanager.qa.CustomerJourneyTest', 'createArabicCustomerValidatesFormSavesOnceAndNavigatesBack'),
    ('com.trader.salesmanager.qa.CustomerJourneyTest', 'editCustomerPreservesIdAndDoesNotCreateDuplicate'),
    ('com.trader.salesmanager.qa.PdfFixtureTest', 'generateSingleInvoiceForHostContentVerification'),
    ('com.trader.salesmanager.qa.PdfFixtureTest', 'generateLongInvoiceForHostPaginationAndContentVerification'),
    ('com.trader.salesmanager.qa.PdfFixtureTest', 'generateCustomerStatementForHostBalanceVerification'),
    ('com.trader.salesmanager.qa.PdfFixtureTest', 'generateMonthlyReportForHostPaginationAndFinancialVerification'),
    ('com.trader.salesmanager.qa.PdfFixtureTest', 'exportViewModelGeneratesRealPartialReturnInvoiceForHostVerification'),
}
PDFBOX = {('com.trader.qa.InvoicePdfContentTest', name) for name in (
    'generatedPdfContainsInvoiceIdentityArabicAndFinancialTotal',
    'longInvoicePaginatesWithoutDroppingFinalItemOrTotal',
    'customerStatementPreservesPaidDebtTotalsAndBothTransactionIdentities',
    'monthlyReportPreservesFinancialSummaryAndFinalDayAcrossPages',
    'viewModelInvoicePreservesRefundNetAmountAndDerivedCustomerBalances',
)}


def failure_details(case):
    """Extract the available JUnit failure description for public runtime evidence."""
    return ' '.join((case.find(tag).get('message') or case.find(tag).text or '').strip()
                    for tag in ('failure', 'error', 'skipped') if case.find(tag) is not None)
