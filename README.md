# TForwarder Clone
1. ارفع كل الملفات إلى مستودع GitHub (تأكد أن مجلد .github يُرفع).
2. من تبويب Actions شغّل Build APK ثم نزّل app-debug.apk من Artifacts.
3. من my.telegram.org احصل على api_id و api_hash وأدخلهما في التطبيق.

إن فشل تنزيل المكتبة com.github.tdlibx:td:1.8.0 أو لم تُعرف الحزمة org.drinkless.tdlib
جرّب نسخة أخرى من نفس المكتبة، أو استبدل الاستيراد إلى org.drinkless.td.tdlib في
TgClient.kt و Forwarder.kt و MainActivity.kt.
