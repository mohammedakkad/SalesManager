# Actual Android PDF content verification

Robolectric4.14.1 does not implement native PdfDocument (zero handle; document-closed failures). Current4.17 shadow sources were inspected and also contain no PdfDocument implementation. Do not mock a passing PDF or disable assertions.

The offline instrumentation runner refuses real Firebase configuration. On a disposable emulator job, generate the offline packaging fixture, run Seller instrumentation (customer journeys + PDF generation), then pull the actual files and validate them on the host:

```sh
python3 scripts/generate-offline-firebase.py
gradle --configure-on-demand --no-configuration-cache :salesmanager:connectedDebugAndroidTest
adb pull /sdcard/Android/data/com.trader.salesmanager.debug/files/qa-pdf /tmp/seller-qa-pdf
gradle -p qa/pdf-verifier -PpdfFixtureDir=/tmp/seller-qa-pdf test
```

Do not run the fixture generator over a user's existing config; use a disposable CI checkout. These tests assert actual PDF signature, page count, invoice ID, Arabic extracted text, financial total and retention of the final item on a multipage invoice. They are BLOCKED/NOT TESTED locally until a device supplies PDFs; test compilation is not an execution pass. Android PdfRenderer also checks the actual generated files on device. Physical print/share layout and RTL visual accuracy require further device review.
