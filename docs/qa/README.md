# QA — الحالة الحالية

آخر تحديث: 2026-11-21 (فرع `chore/modernization-2026-11`).

| البند | الحالة |
|---|---|
| اختبارات JVM | `:app` 285 + `:salesmanager` 820 (جديد: محادثة 2، مظهر 1، PDF 13، جرد الواجهة 11) — كلها ناجحة محلياً |
| بوابات التغطية (`scripts/report-seller-coverage.py --check`) | ناجحة |
| Lint (`:salesmanager:lintDebug`) | 0 أخطاء؛ التحذيرات مفصّلة في [seller-lint-triage.md](seller-lint-triage.md) |
| البناء | debug / staging / release مصغّر (R8) لـ `:salesmanager`، و release لـ `:admin` — ناجح |
| اختبارات الأجهزة و PDFBox | تعمل في CI (`seller-qa.yml`، API 24/28/33/35/36)؛ لا يوجد KVM في بيئة التطوير المحلية |
| جاهزية الإصدار | **FAIL** — العوائق الموثّقة سابقاً ما زالت قائمة (تفويض/هوية على الخادم، عزل المستأجر القديم، حدود الترحيل التاريخي). انظر [seller-release-readiness.md](seller-release-readiness.md) و[قائمة الأسرار](../security/secrets-checklist-ar.md) |

## الوثائق الحالية
- [seller-release-readiness.md](seller-release-readiness.md) — الجاهزية والعوائق
- [seller-security-review.md](seller-security-review.md) — المراجعة الأمنية
- [seller-bug-audit.md](seller-bug-audit.md) — تدقيق الأخطاء
- [seller-lint-triage.md](seller-lint-triage.md) — فرز تحذيرات lint
- [seller-test-strategy.md](seller-test-strategy.md)، [seller-coverage-report.md](seller-coverage-report.md)
- [stage11-outbox.md](stage11-outbox.md) — صندوق الصادر (outbox) والمزامنة
- [../modernization/report-ar.md](../modernization/report-ar.md) — تقرير التحديث

## الأرشيف
ملفات التقدّم المرحلية (`*progress*.md`، 22 ملفاً) نُقلت كما هي إلى [archive/](archive/) دون حذف أي محتوى؛ الروابط داخلها حُدّثت لتبقى صالحة.
