# Sales Manager 📊

تطبيق أندرويد لإدارة المبيعات والزبائن والديون للتجار.

## التقنيات
- **Kotlin** + **Jetpack Compose**
- **MVVM** + Clean Architecture
- **Room DB** (تخزين محلي)
- **Koin** (Dependency Injection)
- **Coroutines** + **StateFlow**
- **Firebase** (Crashlytics + Activation)
- **GitHub Actions** (CI/CD → Release APK)

## الشاشات
- ✅ شاشة التفعيل (Firebase Realtime DB)
- ✅ الرئيسية (Dashboard)
- ✅ الزبائن (قائمة + إضافة + تفاصيل)
- ✅ العمليات (قائمة + إضافة + تعديل + تفاصيل)
- ✅ الديون
- ✅ طرق الدفع
- ✅ الإعدادات

## الإعداد

### 1. Firebase
1. أنشئ مشروع في [Firebase Console](https://console.firebase.google.com)
2. فعّل **Crashlytics** و **Realtime Database**
3. في Realtime DB أضف:
```json
{
  "activation_codes": {
    "YOUR_CODE_HERE": true
  }
}
```
4. حمّل `google-services.json` وضعه في مجلد `app/`

### 2. Keystore (للـ release)
```bash
keytool -genkey -v -keystore keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias mykey
```

### 3. GitHub Secrets
أضف هذه الـ Secrets في repository settings:
| Secret | القيمة |
|--------|--------|
| `GOOGLE_SERVICES_JSON` | محتوى ملف google-services.json |
| `KEYSTORE_BASE64` | `base64 keystore.jks` |
| `KEYSTORE_PASSWORD` | كلمة مرور الـ keystore |
| `KEY_ALIAS` | اسم الـ alias |
| `KEY_PASSWORD` | كلمة مرور الـ key |

### 4. Build من Termux
```bash
cd SalesManager
# أضف google-services.json أولاً
./gradlew assembleDebug
```

## الـ Push من Termux
```bash
git init
git remote add origin https://github.com/USERNAME/SalesManager.git
git add .
git commit -m "Initial commit"
git push -u origin main
# لعمل release:
git tag v1.0.0
git push origin v1.0.0
```
