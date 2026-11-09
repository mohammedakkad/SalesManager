"""Named runtime requirements; shared by XML validation and public evidence summaries."""
INSTRUMENTATION = {
    ('com.trader.salesmanager.qa.CustomerJourneyTest', 'createArabicCustomerValidatesFormSavesOnceAndNavigatesBack'),
    ('com.trader.salesmanager.qa.CustomerJourneyTest', 'editCustomerPreservesIdAndDoesNotCreateDuplicate'),
    ('com.trader.salesmanager.qa.PdfFixtureTest', 'generateSingleInvoiceForHostContentVerification'),
    ('com.trader.salesmanager.qa.PdfFixtureTest', 'generateLongInvoiceForHostPaginationAndContentVerification'),
    ('com.trader.salesmanager.qa.PdfFixtureTest', 'generateCustomerStatementForHostBalanceVerification'),
    ('com.trader.salesmanager.qa.PdfFixtureTest', 'generateMonthlyReportForHostPaginationAndFinancialVerification'),
}
PDFBOX = {('com.trader.qa.InvoicePdfContentTest', name) for name in (
    'generatedPdfContainsInvoiceIdentityArabicAndFinancialTotal',
    'longInvoicePaginatesWithoutDroppingFinalItemOrTotal',
    'customerStatementPreservesPaidDebtTotalsAndBothTransactionIdentities',
    'monthlyReportPreservesFinancialSummaryAndFinalDayAcrossPages',
)}
