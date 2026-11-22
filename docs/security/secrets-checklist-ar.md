# قائمة تحقّق الأسرار والإعدادات الحسّاسة (لصاحب المشروع)

> هذه القائمة **لا تغيّر شيئاً تلقائياً**. لم يتم تدوير أي مفتاح، ولم يُعَد كتابة تاريخ git، ولم تُعرض قيم أي ملف. كل بند يحتاج قراراً أو إجراءً من صاحب المشروع في Google Cloud / Firebase / GitHub.

تاريخ المراجعة: 2026-11-21 — فرع `chore/modernization-2026-11`.

## 1. ما تم العثور عليه (دليل بدون قيم)

| # | البند | الدليل | الخطورة |
|---|---|---|---|
| 1 | ملف `admin/google-services.json` مُتتبَّع في مستودع عام | `git ls-files` — يحوي مشروع Firebase واحداً، عميلين (`com.trader.admin`, `com.trader.salesmanager`)، مفتاحَي API، و4 عملاء OAuth | متوسطة |
| 2 | ملف `salesmanager/google-services.json` مُتتبَّع في مستودع عام | `git ls-files` — نفس المشروع، 4 عملاء (`.admin`, `salesmanager`, `.debug`, `.test`)، 4 مفاتيح API، 8 عملاء OAuth، ورابط Realtime Database | متوسطة |
| 3 | **ملف keystore مُرمَّز Base64 موجود في تاريخ git** | الكومِت `7a267e6` (2026-05-20، "fix sync state") أضاف `keystore_base64.txt` (حجمه ≈ 3.7 KB). الملف غير موجود في HEAD لكنه **ما زال في تاريخ الفرع `production`** | **عالية** |
| 4 | سير عمل `release-salesmanager.yml` يعمل عند **كل push لأي فرع** ويفكّ تشفير الـ keystore ويبني APK موقّعاً | `on.push.branches: ['**']` | منخفضة–متوسطة |

ملاحظة: مفاتيح API في `google-services.json` ليست "كلمات سر" بحد ذاتها (هي معرّفات عميل)، لكن **بدون تقييد** يمكن لأي أحد استخدامها لاستدعاء خدمات المشروع. الحماية الحقيقية = تقييد المفاتيح + App Check + قواعد قاعدة البيانات.

## 2. الإجراءات المطلوبة

### أ. تقييد مفاتيح API في Google Cloud Console
- [ ] افتح **APIs & Services → Credentials** لمشروع Firebase.
- [ ] لكل مفتاح Android: اختر **Application restrictions → Android apps** وأضف كل اسم حزمة مع **بصمة SHA-1** لشهادة التوقيع:
  - `com.trader.salesmanager` (release / upload key)
  - `com.trader.salesmanager.test` (staging)
  - `com.trader.salesmanager.debug` (debug — بصمة debug keystore لكل مطوّر)
  - `com.trader.admin`
- [ ] **API restrictions**: اسمح فقط بالـ APIs المستخدمة فعلاً (Firebase Realtime Database, Cloud Firestore, Firebase Installations, FCM Registration, Firebase Remote Config, Identity Toolkit للمصادقة في تطبيق الإدارة، Crashlytics/Analytics).
- [ ] بعد اعتماد Play App Signing أضف بصمة **مفتاح التوقيع الخاص بـ Google Play** أيضاً.

### ب. تفعيل Firebase App Check
- [ ] فعّل App Check بمزوّد **Play Integrity** للتطبيقين.
- [ ] فعّل **enforcement** على Realtime Database و Firestore (ابدأ بوضع المراقبة Metrics أولاً لأيام قليلة).
- [ ] ملاحظة: هذا يتطلب إضافة `firebase-appcheck-playintegrity` وتهيئته في الكود — لم يُضَف في هذا الفرع لأنه يغيّر سلوك الاتصال بالخادم (قرار منتج).

### ج. مراجعة قواعد Realtime Database و Firestore
القواعد **غير موجودة في المستودع** (لا يوجد `database.rules.json` / `firestore.rules`)، لذا لا يمكن التحقق منها من الكود. يجب مراجعتها في Firebase Console:
- [ ] **`activation_codes/{code}`**: التطبيق يقرأ هذا المسار مباشرة بالكود (`ActivationRepositoryImpl`, `MerchantStatusRepositoryImpl`, `FirebaseSubscriptionRepositoryImpl`). تأكّد أن:
  - لا يوجد `".read": true` على الجذر `activation_codes` (يمنع سرد كل الأكواد).
  - الكتابة على حالة الاشتراك/الصلاحية **للمشرف فقط** (`auth.token.admin == true` أو UID محدد).
  - الأكواد طويلة وعشوائية بما يكفي لمنع التخمين.
- [ ] مسارات بيانات التاجر (`merchants/{merchantId}/...` في Firestore والمسارات المقابلة في RTDB): القراءة/الكتابة يجب أن تكون مقيّدة بهوية التاجر، لا بمجرد معرفة `merchantId`. (تقارير QA السابقة تذكر "server authorization/identity" كعائق إصدار — هذا هو البند نفسه.)
- [ ] `chat/{merchantId}/messages`: تأكد أن التاجر لا يستطيع قراءة محادثات تاجر آخر.
- [ ] احفظ القواعد في المستودع (`firebase.json` + ملفات القواعد) لتصبح قابلة للمراجعة والاختبار.

### د. مفتاح التوقيع المسرّب في التاريخ (البند 3)
- [ ] **اعتبر المفتاح الموجود في `7a267e6` مكشوفاً** (المستودع عام؛ أي نسخة/fork/cache قد تحتفظ به).
- [ ] بما أن التطبيق **لم يُنشر بعد على Google Play**: أنشئ **مفتاح رفع (upload key) جديداً** قبل أول رفع، وفعّل **Play App Signing** (Google تحتفظ بمفتاح التوقيع النهائي).
- [ ] حدّث أسرار GitHub: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` بالمفتاح الجديد، واستخدم كلمات سر جديدة.
- [ ] المستخدمون الحاليون الذين ثبّتوا APK من GitHub Releases موقّعاً بالمفتاح القديم: الانتقال لمفتاح جديد يمنع التحديث فوق التثبيت القديم (يلزم إلغاء التثبيت أو استخدام APK Signature Scheme v3 key rotation). **قرار منتج.**
- [ ] **ملاحظة المرحلة 6 (توقيت التدوير):** التطبيق يتحدّث حالياً من GitHub Releases، لذلك تغيير مفتاح التوقيع يعني أن المختبرين الحاليين سيعيدون تثبيت التطبيق **مرة واحدة** (التحديث فوق نسخة موقّعة بالمفتاح القديم سيُرفض). لذلك: دوّر المفتاح **قبل التوزيع الأوسع وقبل أول رفع إلى Google Play** (مع تفعيل Play App Signing عند التسجيل)، ثم حدّث أسرار GitHub Actions (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). لم يُدوَّر أي مفتاح في هذا الفرع.
- [ ] (اختياري، قرار المالك) إزالة الملف من التاريخ بـ `git filter-repo` ثم force-push — **لا يُلغي** النسخ الموجودة مسبقاً، فتدوير المفتاح يبقى ضرورياً.
- [ ] افحص التاريخ الكامل (هذه النسخة كانت clone سطحياً بعمق 100 كومِت):
  ```bash
  git clone --mirror https://github.com/mohammedakkad/SalesManager.git
  ```
  ```bash
  git -C SalesManager.git log --all --name-status --format='%h %ad %s' --date=short -- '*.jks' '*.keystore' '*.p12' 'keystore*' '*keystore_base64*'
  ```

### هـ. ملفات `google-services.json`
- [ ] الملفان موجودان أصلاً في `.gitignore` لكنهما مُتتبَّعان. سير عمل الإصدار يحقنهما من الأسرار (`GOOGLE_SERVICES_JSON`, `ADMIN_GOOGLE_SERVICES_JSON`)، وسير عمل QA يولّد ملفاً وهمياً (`scripts/generate-offline-firebase.py`). لذلك **يمكن** إيقاف تتبّعهما بأمان من ناحية CI:
  ```bash
  git rm --cached admin/google-services.json salesmanager/google-services.json
  ```
  لم يُنفَّذ هذا لأن البناء المحلي (مثل Termux) يعتمد على وجود الملف — احتفظ بنسخة محلية أولاً.
- [ ] إيقاف التتبّع لا يزيل القيم من التاريخ؛ الحماية الفعلية هي البنود أ–ج.

### و. سير عمل الإصدار
- [ ] قيّد `release-salesmanager.yml` و `release-admin.yml` بالتاجات (`salesmanager-v*`, `salesmanagertest-v*`) و `workflow_dispatch` بدل `branches: ['**']`، لتقليل عدد مرات فكّ تشفير الـ keystore. (لم يُغيَّر لأنه يغيّر طريقة عمل المالك الحالية.)

## 3. ما تم فعله في هذا الفرع
- لا تدوير، لا حذف، لا إعادة كتابة للتاريخ.
- `scripts/check-seller-secrets.py` ما زال يمر (PASS).
- تم توثيق البنود أعلاه فقط.
