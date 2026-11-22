# Sales Manager 📊

تطبيق أندرويد لإدارة المبيعات والزبائن والديون والمخزون للتجار، مع تطبيق إدارة منفصل للمشرف. الواجهة عربية (RTL).

## الوحدات (Gradle modules)

| الوحدة | الحزمة | الوصف |
|---|---|---|
| `:app` | `com.trader.core` | النواة المشتركة: Room، المستودعات، المزامنة مع Firebase، العمّال (WorkManager)، الأدوات |
| `:salesmanager` | `com.trader.salesmanager` | تطبيق التاجر |
| `:admin` | `com.trader.admin` | تطبيق المشرف (التجّار، الاشتراكات، المحادثات) |

## التقنيات

- **Kotlin 2.4** + **Jetpack Compose** (Material 3)، Clean Architecture + MVVM
- **Room 2.8** (تخزين محلي مع ترحيلات ومخططات مُصدَّرة في `app/schemas`)
- **Koin 4** لحقن الاعتماديات، **Coroutines / Flow**
- **Firebase** (BoM 35): Realtime Database، Firestore، Auth، Messaging، Remote Config، Crashlytics، Analytics
- **WorkManager**، **DataStore**، **CameraX + ML Kit** (مسح الباركود)، **OkHttp**، **Coil 3** (تطبيق الإدارة)، **Vico** (رسم بياني في لوحة التحكم)
- تصدير **PDF** (فواتير، كشوف حساب، تقارير شهرية) و Excel
- البناء: **Gradle 9.9 (wrapper)**، **AGP 9.5** مع Kotlin المدمج، **compileSdk/targetSdk 36**، minSdk 24، JDK 17+
- جميع الإصدارات في `gradle/libs.versions.toml`

## الشاشات (تطبيق التاجر)

- التفعيل (كود التاجر عبر Firebase)
- الرئيسية: لوحة تحليلات، حالة المزامنة
- الزبائن: قائمة، إضافة/تعديل، تفاصيل + كشف حساب PDF
- العمليات (الفواتير): قائمة، إضافة/تعديل مع أصناف المخزون، تفاصيل + فاتورة PDF، المرتجعات
- الديون، عمليات يوم محدد
- التقارير: تقرير شهري PDF/Excel، تقارير المخزون
- المخزون: المنتجات والوحدات، تفاصيل المنتج وحركات المخزون، جلسات الجرد، مسح الباركود
- الصناديق النقدية وحركاتها، طرق الدفع
- الموظفون وصلاحياتهم، الجلسات النشطة
- النسخ الاحتياطي المشفّر والاستعادة
- المحادثة مع الإدارة، الإعدادات، التحديث داخل التطبيق (APK من GitHub Releases)

## الإعداد المحلي

### 1. Firebase
1. أنشئ مشروعاً في [Firebase Console](https://console.firebase.google.com) وفعّل Realtime Database و Firestore و Crashlytics و Cloud Messaging.
2. سجّل التطبيقات: `com.trader.salesmanager` (و `.debug` و `.test` إن أردت بناء debug/staging) و `com.trader.admin`.
3. ضع ملفات `google-services.json` في:
   - `salesmanager/google-services.json`
   - `admin/google-services.json`
4. راجع [قائمة تحقّق الأسرار](docs/security/secrets-checklist-ar.md) (تقييد المفاتيح، App Check، قواعد قاعدة البيانات).

### 2. Android SDK
SDK Platform 36 و Build-Tools 36.0.0، مع `sdk.dir` في `local.properties` أو المتغير `ANDROID_HOME`.

### 3. التوقيع (release / staging)
إما ملف `keystore.properties` في الجذر (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`) أو متغيرات البيئة `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. بدونها يُبنى `:salesmanager` كـ APK غير موقّع، بينما `:admin` يتطلب بيانات التوقيع.

### 4. البناء والاختبار
```bash
./gradlew :salesmanager:assembleDebug
```
```bash
./gradlew :salesmanager:assembleStaging
```
```bash
./gradlew :salesmanager:assembleRelease
```
```bash
./gradlew :app:testDebugUnitTest :salesmanager:testDebugUnitTest
```
حزمة QA الكاملة كما في CI (اختبارات + تغطية + lint + بناء debug):
```bash
GRADLE_CMD=./gradlew scripts/qa-seller.sh
```
تفاصيل اختبار ملفات PDF على جهاز/محاكي: `qa/pdf-verifier/README.md`.

## CI/CD (GitHub Actions)

| Workflow | الوظيفة |
|---|---|
| `seller-qa.yml` | اختبارات JVM، التغطية، lint، بناء debug و release مصغّر غير موقّع، واختبارات أجهزة على API 24/28/33/35/36 |
| `release-salesmanager.yml` | APK موقّع؛ التاج `salesmanager-vX.Y.Z` = إصدار، `salesmanagertest-vX.Y.Z` = staging |
| `release-admin.yml` | APK موقّع لتطبيق الإدارة |

الأسرار المطلوبة في GitHub:

| Secret | القيمة |
|---|---|
| `GOOGLE_SERVICES_JSON` | محتوى `salesmanager/google-services.json` بترميز base64 |
| `ADMIN_GOOGLE_SERVICES_JSON` | محتوى `admin/google-services.json` بترميز base64 |
| `KEYSTORE_BASE64` | `base64 keystore.jks` |
| `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` | بيانات المفتاح |

## التوثيق
- حالة الجاهزية والـ QA الحالية: [`docs/qa/README.md`](docs/qa/README.md)
- تقرير التحديث (نوفمبر 2026): [`docs/modernization/report-ar.md`](docs/modernization/report-ar.md)
