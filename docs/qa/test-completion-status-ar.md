# حالة الاختبارات والتقدم نحو 100%

تاريخ الجرد: 2026-11-09. لم تتحقق 100%، وجاهزية الإصدار FAIL. لا تعني تغطية الأسطر ضمان صحة الأعمال أو أمان الخادم. بقيت بوابات JVM عند 70% وسياسة إدراج كود الإنتاج كما هي؛ لم نضف استثناءات أو ملفات PDF وهمية لرفع النسبة.

بدأت هذه المرحلة من 94a2e48fee725e3a717cc133e36b4682ddd05639 على codex/seller-full-coverage-audit و PR51. أضيف 54 اختبار JVM:من 355 إلى 409، مع الاحتفاظ بالاختبارات السابقة. توجد كذلك زيادات منفصلة في اختبارات الجهاز و PDFBox وحراس التغطية. المصدر المؤهل نهائيًا ef1c8999577518e14f1fc799328360eb38721874. اكتمل CI بنجاح، والحفظ التالي مخصص للتوثيق والأدلة فقط. إجمالي الزيادات 66 حالة: 54 للوحدتين JVM، و 6 Python، و 3 جهاز و 3 PDFBox. لا تدخل تجارب Fault injection في هذه الزيادة.

## القياس المحلي الفعلي

| النطاق | أسطر مغطاة | إجمالي الأسطر | النسبة | أسطر غير مغطاة |
|---|---:|---:|---:|---:|
| Seller/JVM |13291|17824|74.5680%|4533|
| shared app/JVM |7594|10269|73.9507%|2675|
| الإجمالي الموزون/JVM |20885|28093|74.3424%|7208|

الموزون هو مجموع الأسطر المغطاة مقسومًا على مجموع الأسطر، وليس متوسط النسبتين. تغير المقام نتيجة إصلاح كود الإنتاج؛ لم يُخفّض بإقصاء ملفات. تشمل السياسة الحالية الأصناف المولّدة وفروع المترجم. والأسطر ذات التنفيذ الجزئي تُحسب مغطاة وفق JaCoCo؛ لا تعني تنفيذ كل شروطها.

الفروع:Seller 6541/14346=45.5946%، والإجمالي 7918/17373=45.5765%. بقي 7805 فروع Seller و 9455 فرعًا موزونًا غير مغطى. لا يمكن تحويل هذه الأعداد إلى «عدد اختبارات مطلوب» بصورة موثوقة: قد يغطي اختبار واحد أسطرًا عديدة، وقد يحتاج شرط واحد حالات وتزامنًا وأعطالًا مختلفة.

أكبر الفجوات تشمل PDF على JVM، وأجزاء التقارير والتنقل، وتركيب الاعتماديات، والكاميرا، وحالات واجهات لم تُمارس. تجربة معزولة فعلية لـ Robolectric4.16/Native Graphics/API28 فشلت عند PdfDocument.startPage برسالة document is closed؛ لم تُحسب اختبار إنتاج ناجحًا ولم نحدّث اعتمادات المشروع أو نستبدل PDFDocument بظل وهمي.

## ما نُفّذ حسب النوع

409 حالات JVM للوحدتين app و Seller ناجحة، 0 فشل/خطأ/تخطٍّ؛ لا تشمل PDFBox المستقلة. التوزيع الأساسي دون تكرار بين الصفوف:

| النوع الأساسي | الحالات الناجحة | حدود الدليل |
|---|---:|---|
| Room/SQLite |188|قاعدة SQLite فعلية داخل حاضنة JVM؛ ليست قتل عملية Android|
| ترحيلات Room |7|النسخ الأصلية المتاحة 22–25؛ لا تؤهل 1–21|
| عقود Firebase/SDK بمحاكيات محددة |22|ليست اختبار قواعد منشورة أو Firebase Emulator|
| منطق ونماذج وحالة واجهة |100|تشمل عقود التصدير والإلغاء ونجاح Excel الحقيقي؛ تصنيف أساسي واحد لكل حالة|
| Compose على JVM |63|ليست مصفوفة أجهزة أو اختبار MainActivity كامل|
| HTTP محلي فعلي |4|MockWebServer على loopback؛ ليس اتصالًا بخدمات إنتاج|
| WorkManager |6|حالات أساسية مستقلة؛ توجد حالات عامل أخرى ضمن Room|
| إشعارات Android على JVM |3|لا تؤهل إذن الإشعارات الفعلي على API33+|
| رسم Native Bitmap |2|لا تعني توليد PDF على JVM|
| صيغة/تشفير النسخ الاحتياطي |10|ليست كل اختبارات الأداء والفوضى أو ترقية APK موقّع|
| محتوى XLSX فعلي |4|فحوص محتوى الملف؛ توجد أيضًا حالات نجاح تصدير ضمن اختبارات الحالة|

الوسوم المتداخلة أوسع من التصنيف الأساسي:195 حالة Room تشمل 7 ترحيلات، و 46 حالة تستخدم SDK/بروتوكول Firebase مزيفًا، و 13 حالة WorkManager، و 19 حالة إعادة فتح/استعادة داخل الحاضنة. لا تجمع هذه الوسوم مع 409 لأنها تتداخل.

11 اختبار Python لحماية سلامة التغطية ناجحة، منفصلة عن 409. تستخدم اختبارات الحراس ملفات XML/EC اصطناعية داخل مجلدات مؤقتة لفحص رفض البيانات غير الصحيحة؛ لا تدخل هذه الملفات إلى تقارير JaCoCo الفعلية أو نسب الإنتاج.

آخر تشغيل نهائي: [42693672936](https://github.com/mohammedakkad/SalesManager/actions/runs/42693672936) عند ef1c8999577518e14f1fc799328360eb38721874. Workflow SUCCESS ومهمتا seller و compose SUCCESS. تظهر نتائج XML الفعلية نجاح 7 اختبارات instrumentation و 5 PDFBox، 0 فشل/خطأ/تخطٍّ. نجحت خطوات الاختبار والتغطية و Lint والبناء المصغّر وحفظ Artifacts صراحةً. لم تُنزّل الملفات الخام بشكل مستقل بسبب حاجز تسجيل الدخول؛ نُشرت الحالات والعدادات ومسارات الملفات و SHA256 من السجلات الفعلية كإشعارات عامة، وحُفظت في JSON الأدلة.

تشمل الملفات فاتورة قصيرة وطويلة وكشف حساب وتقريرًا شهريًا وفاتورة إرجاع من ExportViewModel. يفحص الاختبار الجديد الهوية والإجمالي 100 والمرتجع 25 والصافي 75 والرصيدين 10/85 والإشارة السالبة للمبلغ المرتجع، باستخدام PDFBox على ملف Android الحقيقي.

ثبت أن Context الفعلي هو com.trader.salesmanager.debug. مثال مسار مولّد فعلي:
`/storage/emulated/0/Android/data/com.trader.salesmanager.debug/files/qa-pdf/invoice-return-viewmodel.pdf`.
نُسخت الملفات أثناء instrumentation إلى`/data/local/tmp/seller-qa-pdf/` وفُحصت بصماتها على الجهاز، ثم سُحبت واستُخدمت في PDFBox. هذا يحفظها خارج تخزين الحزمة قبل تنظيف connectedAndroidTest، دون استنتاج package/path ثابت. لا نُثبت سبب كل حالة حذف في التشغيل القديم الذي لم تتوفر سجلاته الخام؛ نثبت الآن الإنشاء والحفظ والفحص الناجح للملفات الخمسة.

القياس الإضافي الفعلي على CI، منفصلًا عن JVM المحلي:

| النطاق Android+JVM | المغطى | المقام | النسبة | غير المغطى |
|---|---:|---:|---:|---:|
| Seller |14295|17824|80.2009%|3529|
| الموزون |21906|28093|77.9767%|6187|

تُرفض بيانات الجهاز الفارغة واختلافات class identity أو المقامات أو فقدان تغطية JVM، ويُشترط تسجيل تنفيذ مولّدات PDF. أسطر InvoicePdfGenerator على الجهاز مع JVM 489/537، والتقرير الشهري 438/457، و PdfCanvas 68/70. لم نعد تسمية هذا القياس ليبدو أنه نسبة JVM الأصلية أو 100%.

إجمالي الحالات المؤهلة عبر المجموعات المنفصلة 432 = 409 للوحدتين + 11 Python + 7 جهاز + 5 PDFBox، 0 فشل/خطأ/تخطٍّ فيها. تكرار تشغيلها داخل CI لا يزيد عدد الحالات. التجربة المعزولة لـ PDF على Robolectric فاشلة ومذكورة منفصلًا، كما أن إخفاقات ما قبل الإصلاح والأخطاء المحقونة ليست إخفاقات متبقية في المصدر المؤهل.

## أعطال أُثبتت قبل إصلاحها

- إعادة محاولة دفتر المخزون FAILED دون تحويله يدويًا إلى PENDING، مع الحفاظ على سياج التاجر وعدم إعادة الأثر المحلي.
- شاشة الدين المفتوحة تتحدث بعد البيع/السداد/الحذف، باستخدام الدين المشتق نفسه من العمليات.
- الاحتفاظ بـ paymentType في الرفع والقراءة والمراقبة مع fallback القديم DEBT.
- رفض تنزيل مقطوع وحذف جزئه، وانتشار الإلغاء وتنظيف التنزيل غير المكتمل.
- تأخر تحميل الفاتورة كان يعرض 150 ويحفظ 160؛ أصبحت المعاينة متفقة مع المبلغ المحفوظ.
- التقرير اليومي كان يستبعد ما بعد 23:59:00؛ أصبح حدّه قبل بداية اليوم التالي.
- نقرتا حفظ الصنف كانتا ترسلان عمليتين؛ أصبح الحجز متزامنًا مع إعادة محاولة مضبوطة بعد الفشل.
- دورة التصدير: حجز Loading فورًا، وعدم تحويل Cancellation/Timeout إلى Error، وعدم إحياء Success بعد reset. حافظت معالجة IO/الذاكرة على رسائل الخطأ وأسبابه، وتنجح إعادة المحاولة بملف Excel فعلي.

هذه ليست تغييرات في معادلات المال أو مخطط قاعدة البيانات. إلغاء التصدير يمنع نشر نتيجة قديمة؛ قد تنتهي كتابة مولّد متزامن في IO إلى ملف cache قبل ملاحظة الإلغاء، ولا ندّعي إيقافه فورًا أو حذفه آليًا. إلغاء تنزيل HTTP تعاوني أيضًا وتبقى مهلة قراءة Socket محدودة.

## ما بقي من الاختبارات

الخطة المحددة الآن 69 مهمة: 36 NOT TESTED و 32 BLOCKED ومهمة PARTIAL واحدة. أغلقت LOG-01/02/04/06 بدليل فشل قبل الإصلاح ونجاح بعده. هذه خطة عمل قابلة للتوسعة، وليست 69 اختبار unit بسيطًا أو قائمة بكل الاختبارات الممكنة للوصول 100%.

| النوع المتبقي | غير منفّذ | محجوب | التفصيل |
|---|---:|---:|---|
| ترحيلات تاريخية |0|21|مسارات 1–21 إلى 25 تحتاج schemas وقواعد مأهولة أصلية؛ لا تُصنع fixtures تاريخية|
| حدود منطقية |4|0|الأرقام غير المحدودة عبر واجهات النقد/الصنف/الإرجاع؛ ردود barcode المتأخرة؛ سياسة استعادة JSON المعيب/الفارغ؛ رفض إلغاء الجلسة|
| أخطاء SDK/listeners |8|0|رفض/إلغاء مراقبة العملاء، العمليات، طرق الدفع، الصناديق، حركات النقد، الأصناف، الإرجاعات، الموظفين|
| تكامل Firebase والخادم |0|8|قواعد الوصول، عضوية التاجر، إلغاء الصلاحيات، عميلان حقيقيان، revisions/receipts، مصالحة Firestore/RTDB، tombstones الكتالوج، الكتابات القديمة|
| مصفوفة Android |3|0|API24 و 33 و 35؛ إعادة تنفيذ المجموعة نفسها على 3 بيئات، وليست 3 حالات unit|
| الجهاز/E2E |9|1|MainActivity وروابط الإشعارات، الكاميرا، قتل العملية أثناء البيع/الإرسال، نفاد القرص/WAL، إذن الإشعارات، RTL والخط الكبير، TalkBack، كشف أكثر من 25 عملية؛ الترقية الموقعة محجوبة|
| الأداء |5|0|زمن Sale commit، تصريف Outbox، نسخ كبيرة، PDF طويل، بدء/تمرير على جهاز ضعيف؛ تحتاج datasets وميزانيات أداء|
| الأمن |3|2|fuzz للنسخ، تسرب البيانات، فحص الاعتمادات؛ التحقق من أصالة APK واختبار صلاحيات الخادم محجوبان|
| Mutation testing |4|0|أربع حملات لم تُنفّذ؛ حملة SaleCommands إضافية PARTIAL بعد العينة المحدودة، وقد تنتج الحملة الكاملة مئات mutants|
| الإجمالي |36|32|ومهمة PARTIAL واحدة؛ 69 مهمة محددة|

الترقية الموقعة تحتاج APK سابقًا مُجازًا وبصمات شهادات عامة، لا مفاتيح الإنتاج. و Firebase Emulator غير مؤهل هنا: تحميل الملف الرسمي أعاد 403 وفق سياسة الشبكة، كما أن مصدر القواعد وربط UID بالتاجر غير متاحين. روابط Console للبيانات لا تثبت عقد التفويض. لا نختبر ضد Firebase الإنتاج.


## عينة Fault injection/Mutation فعلية

نُفّذت 8 أخطاء مختارة مسبقًا في git archive معزول للمصدر ef1c899، دون تعديل ملفات الإنتاج أو تقارير التغطية في الفرع. نجحت 8 حالات Room مختارة في خط الأساس؛ كشفت الاختبارات الأخطاء الثمانية، بإجمالي 10 إخفاقات مقصودة أثناء اختبار النسخ المعطوبة، 0 mutants ناجية و 0 invalid. شملت إسقاط الأصناف، وعكس المخزون، وإسقاط النقد، والسماح بالمخزون السالب، وتغيير طلب تحت operationId مستخدم، وقبول تعديل قديم، وإعادة مخزون مُرجع، وإسقاط Outbox. لا يُحسب فشل compile كاكتشاف؛ جميع الإخفاقات كانت أثناء تنفيذ الاختبارات.

أُعيد المصدر المعزول حرفيًا وتحققت بصمة المصدر الأصلي وعدّادات تغطيته دون تغيير. هذه عينة حساسية محددة، وليست مقياس Mutation شاملًا أو 8 اختبارات إنتاج جديدة أو زيادة في تغطية الأسطر. لذلك حالة حملة SaleCommands PARTIAL وتبقى 4 حملات أخرى NOT TESTED. الأدلة و XML لكل نسخة في sale-mutation-sample/.

## الذرّية و Outbox وقاعدة البيانات

SaleCommands يكتب العملية والأصناف والمخزون ودفتره والنقد والدين المشتق ونية المزامنة داخل Room transaction واحدة. يبقى الإرسال بعد commit. تحتوي المجموعة على أعطال SQL وإلغاء وتزامن ومخزون أخير وتكرار وإرجاعات وحذف وإعادة فتح حقيقية. لا تعني حاضنة إعادة الفتح اختبار OS process kill.

Outbox المحلي و claim/lease/retry والتاجر والمراجعة والإقرار محمية وتُختبر محليًا. لا توجد معاملة واحدة بين Firestore و RTDB، ولا ضمان exactly-once عالمي دون deduplication وسياج revision وتفويض مفروض من الخادم. لا ندّعي حل كل كاتب قديم أو كل cache مشترك بين الحسابات. مكتبة Room الفعلية 2.6.1 وإصدار المخطط 25؛ الملفات الأصلية 22.json إلى 25.json متاحة. لم نضف migration في هذه المرحلة، وتبقى 1–21 محجوبة.

## البناء وقرار الإصدار

JVM/JaCoCo/gates ناجحة. Lint النهائي 0 أخطاء و 24 تحذيرًا؛ بُنيت Debug و AndroidTest و Release المصغّر غير الموقّع لإصلاح التصدير النهائي بنجاح ضمن تشغيل 5 د 14 ث. ليست هذه شهادة تشغيل Release موقّع أو ترقية فعلية. فحص الأسرار المستهدف ناجح؛ لا يساوي فحصًا أمنيًا شاملًا.

تبقى P0/P1: تفويض وتسييج الخادم، المصالحة بين قاعدتي Firebase، ملكية وتهيئة بيانات التجار والكتابات القديمة، معرّفات عابرة للأجهزة، fixtures التاريخية، الترقية/أصالة APK، والتأهيل الأمني والأدائي. الوصول لتغطية أسطر 100%، إن أمكن لاحقًا، لا يغلق هذه المخاطر تلقائيًا. لم نعدل Admin business logic أو نصل إلى بيانات إنتاج أو نستخدم توقيع إنتاج؛ العمل على الفرع و PR الموجودين بلا merge أو force-push.

الأدلة:[التأهيل النهائي المهيكل](evidence/continuation/coverage100/final/qualification.json)،[الجرد الفعلي](evidence/continuation/coverage100/final/test-inventory.json)،[الخطة المتبقية](remaining-test-plan.csv)،[فجوات الأسطر والفروع](seller-coverage-gaps.csv)،[أدلة CI](evidence/continuation/coverage100/hosted/)،[تجربة PDF المعزولة الفاشلة](evidence/continuation/coverage100/pdf-runtime-416-probe/)،[الذرية و Outbox](stage8-atomicity-and-outbox.md).


## Seller form and restoration continuation — 2026-11-10

Starting HEAD: 78bc60543d490cf47317afb832f3d5bf1df6d43e, same audit branch and PR51. Complete local acceptance has 441 app/Seller JVM cases PASS (shared135/Seller306), 0 failures/errors/skips, with all409 prior identities preserved and32 new cases. The unchanged shared test task uses its previously executed cached result; Seller306 executes in this acceptance. Eleven Python coverage guards PASS. Seller JVM lines13439/17862=75.2379%; weighted21033/28131=74.7680%; weighted branches8053/17473=46.0883%. The original70% floors/inclusion policy remain unchanged. Target100% remains NOT ACHIEVED. Actual new-source hosted/device runtime is pending, not inferred from compiled APKs.

Nineteen pre-repair failing regression conditions are now repaired: barcode debounce claims checking synchronously, rejects obsolete owner/value/revision callbacks and surfaces query failure; product forms reject overflow in price/cost/quantity/threshold, preserve a typed zero threshold (previously silently became default5), and send deletion intent only for actually persisted units. Existing finite price/cost/stock arithmetic, blank optional defaults and weight/unit identities are preserved.

Session revocation now retains the local row on rejected remote removal, reports failure, deduplicates in-flight requests and permits explicit retry. Eight real Room/Compose cases use deterministic Firebase Tasks, including actual SQL abort after a mocked remote acknowledgment. Cancellation does not become false failure. This is a local client contract, not proof of deployed server authorization or atomicity between Firebase and SQLite; uncertain remote completion/local SQL failure can still require reconciliation.

Invoice restoration now validates the complete selection before replacing the draft, keeps the prior invoice/intent on malformed/empty/unavailable input, rejects stale completions and preserves cancellation. Save is disabled/guarded while restoration is active, preventing commitment of the old invoice before the selected items are ready. The item editor already forbids confirming an empty list; recovery reports an explicit message instead of interpreting missing navigation data as deletion. Legacy weight fallback, gross/base/refund arithmetic and stable operation/item identities remain intact. No shared/Admin business logic, database schema or network protocol changed.

Complete JaCoCo/gates/Lint/Debug/AndroidTest/minified unsigned Release acceptance PASS in5m9s. Lint0 errors/24 warnings; targeted secret policy PASS; release signature verification rejects the expected unsigned QA APK. Evidence: coverage-next/{barcode,product-draft,session-revoke,invoice-draft,final}/. The explicit remaining plan is66 work items:32 NOT TESTED,32 BLOCKED,2 PARTIAL; LOG-05/07/08 close, LOG-03 is partial for product UI while cash/return UI remain unqualified. Historical1–21, deployed server fencing/authorization, legacy tenant/cache ownership, distributed reconciliation and signed/security/device/performance blockers remain. Release readiness FAIL.
