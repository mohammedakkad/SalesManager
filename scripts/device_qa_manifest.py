"""Named runtime requirements; shared by XML validation and public evidence summaries."""
INSTRUMENTATION = {
    ('com.trader.salesmanager.qa.LongCustomerStatementTest', 'generateSixtyRowCustomerStatementForActualHostHistoryVerification'),
    ('com.trader.salesmanager.qa.FinancialPdfBoundaryTest', 'generatePaidManualBankInvoiceWithoutItemsForActualHostVerification'),
    ('com.trader.salesmanager.qa.FinancialPdfBoundaryTest', 'generateFullyReturnedCashInvoiceWithZeroNetForActualHostVerification'),
    ('com.trader.salesmanager.qa.FinancialPdfBoundaryTest', 'generateFractionalWeightReturnWithLogoForActualHostVerification'),
    ('com.trader.salesmanager.qa.FinancialPdfBoundaryTest', 'generateEmptyMonthWithoutInventingFinancialActivityForActualHostVerification'),
    ('com.trader.salesmanager.qa.ThemedDialogJourneyTest', 'failedStockAdjustmentRetainsDraftAndExplicitRetryUsesTheEditedQuantity'),
    ('com.trader.salesmanager.qa.ThemedDialogJourneyTest', 'encryptedImportPasswordSurvivesThemeAndUnlocksWithoutReplacingRoom'),
    ('com.trader.salesmanager.qa.ThemedDialogJourneyTest', 'clearingACountAfterThemeChangeOnlyClearsTheSelectedRoomItem'),
    ('com.trader.salesmanager.qa.ThemedDialogJourneyTest', 'actualAndroidEncryptedArchiveRejectsWrongPasswordsAndModifiedCiphertext'),
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
PDFBOX |= {('com.trader.qa.LongCustomerStatementContentTest', 'completeStatementKeepsAllSixtyRowsAndUnchangedFinancialTotalsAcrossPages')}
PDFBOX |= {('com.trader.qa.FinancialPdfBoundaryContentTest', name) for name in (
    'paidManualBankInvoiceKeepsIdentityPaymentAndEmptyItemExplanation',
    'fullyReturnedInvoiceKeepsOriginalRefundAndZeroNetWithoutNegativeQuantity',
    'fractionalReturnPreservesNetWeightRefundRoundingAndActualLogoImage',
    'emptyMonthKeepsZeroTotalsAndExplicitNoSalesMessage',
)}


def failure_details(case):
    """Extract the available JUnit failure description for public runtime evidence."""
    return ' '.join((case.find(tag).get('message') or case.find(tag).text or '').strip()
                    for tag in ('failure', 'error', 'skipped') if case.find(tag) is not None)


def annotation_chunks(rows, limit=3500):
    """Keep complete JSON rows below GitHub's annotation UTF8 byte limit."""
    import json

    def encode(items):
        return json.dumps(items, ensure_ascii=False).replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')

    chunks, current = [], []
    for row in rows:
        if len(encode([row]).encode('utf-8')) > limit:
            raise ValueError('Single annotation row exceeds byte budget')
        if current and len(encode(current + [row]).encode('utf-8')) > limit:
            chunks.append(encode(current))
            current = []
        current.append(row)
    if current or not chunks:
        chunks.append(encode(current))
    return chunks
