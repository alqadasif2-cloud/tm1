package com.example.tforwarder

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.drinkless.tdlib.TdApi
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var root: LinearLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun tv(t: String, size: Float = 16f) = TextView(this).apply {
        text = t; textSize = size; setPadding(0, dp(8), 0, dp(8)); root.addView(this)
    }

    private fun edit(hint: String, initial: String = "", type: Int = InputType.TYPE_CLASS_TEXT) =
        EditText(this).apply {
            this.hint = hint; setText(initial); inputType = type; root.addView(this)
        }

    private fun btn(t: String, onClick: () -> Unit) = Button(this).apply {
        text = t; setOnClickListener { onClick() }; root.addView(this)
    }

    private fun toast(s: String) = runOnUiThread { Toast.makeText(this, s, Toast.LENGTH_LONG).show() }

    /** يرسل أمر لتيليجرام ويعرض الخطأ إن وُجد */
    private fun call(f: TdApi.Function<*>) {
        TgClient.send(f) { r -> if (r is TdApi.Error) toast(r.message) }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val sv = ScrollView(this)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)) }
        sv.addView(root)
        setContentView(sv)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        TgClient.listener = { runOnUiThread { render() } }
        TgClient.start()
        if (Store.running) startSvc()
        render()
    }

    override fun onResume() { super.onResume(); render() }

    private fun startSvc() = ContextCompat.startForegroundService(this, Intent(this, ForwardService::class.java))

    private fun render() {
        root.removeAllViews()
        if (Store.apiId == 0) return setupScreen()
        when (TgClient.auth) {
            is TdApi.AuthorizationStateWaitPhoneNumber -> phoneScreen()
            is TdApi.AuthorizationStateWaitCode -> codeScreen()
            is TdApi.AuthorizationStateWaitPassword -> passScreen()
            is TdApi.AuthorizationStateReady -> homeScreen()
            else -> tv("جاري الاتصال بتيليجرام...")
        }
    }

    private fun setupScreen() {
        tv("إعداد الاتصال", 22f)
        tv("احصل على api_id و api_hash من الموقع my.telegram.org ثم API development tools")
        val id = edit("api_id", "", InputType.TYPE_CLASS_NUMBER)
        val h = edit("api_hash")
        btn("حفظ ومتابعة") {
            val i = id.text.toString().trim().toIntOrNull()
            val hs = h.text.toString().trim()
            if (i == null || hs.isEmpty()) return@btn toast("أدخل القيمتين بشكل صحيح")
            Store.apiId = i; Store.apiHash = hs
            TgClient.start(); render()
        }
    }

    private fun phoneScreen() {
        tv("تسجيل الدخول", 22f)
        tv("أدخل رقم هاتفك مع رمز الدولة")
        val p = edit("+9665XXXXXXXX", "", InputType.TYPE_CLASS_PHONE)
        btn("إرسال كود التأكيد") {
            val f = TdApi.SetAuthenticationPhoneNumber()
            f.phoneNumber = p.text.toString().trim()
            call(f)
        }
    }

    private fun codeScreen() {
        tv("كود التأكيد", 22f)
        tv("أدخل الكود الذي وصلك على تيليجرام")
        val c = edit("الكود", "", InputType.TYPE_CLASS_NUMBER)
        btn("تأكيد") {
            val f = TdApi.CheckAuthenticationCode()
            f.code = c.text.toString().trim()
            call(f)
        }
    }

    private fun passScreen() {
        tv("التحقق بخطوتين", 22f)
        val p = edit("كلمة المرور", "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        btn("دخول") {
            val f = TdApi.CheckAuthenticationPassword()
            f.password = p.text.toString()
            call(f)
        }
    }

    private fun homeScreen() {
        tv("تم التوجيه اليوم: ${Store.today()}", 20f)
        Switch(this).apply {
            text = "تشغيل التوجيه التلقائي"
            isChecked = Store.running
            setOnCheckedChangeListener { _, on ->
                Store.running = on
                if (on) startSvc() else stopService(Intent(this@MainActivity, ForwardService::class.java))
            }
            root.addView(this)
        }
        tv("القواعد", 20f)
        Store.rules().forEach { r ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, dp(8)) }
            row.addView(TextView(this).apply {
                text = "${r.name}\nمن ${r.sources.size} مصدر ← ${TgClient.title(r.dest)}"
                textSize = 16f
            })
            row.addView(Switch(this).apply {
                text = "مفعّلة"; isChecked = r.enabled
                setOnCheckedChangeListener { _, on -> Store.upsert(r.copy(enabled = on)) }
            })
            row.addView(Button(this).apply { text = "تعديل"; setOnClickListener { ruleEditor(r) } })
            row.addView(Button(this).apply {
                text = "حذف"
                setOnClickListener { Store.delete(r.id); render() }
            })
            root.addView(row)
        }
        btn("+ قاعدة جديدة") { ruleEditor(null) }
        btn("تسجيل الخروج") {
            Store.running = false
            stopService(Intent(this, ForwardService::class.java))
            call(TdApi.LogOut())
        }
    }

    private fun ruleEditor(old: Rule?) {
        root.removeAllViews()
        val sources = old?.sources?.toMutableList() ?: mutableListOf()
        var dest = old?.dest ?: 0L
        tv(if (old == null) "قاعدة جديدة" else "تعديل القاعدة", 22f)
        val name = edit("اسم القاعدة", old?.name ?: "")
        val srcBtn = btn("") {}
        val dstBtn = btn("") {}
        fun refresh() {
            srcBtn.text = "اختيار المصادر (${sources.size})"
            dstBtn.text = "الوجهة: ${TgClient.title(dest)}"
        }
        refresh()
        srcBtn.setOnClickListener {
            TgClient.loadChats { chats ->
                runOnUiThread {
                    if (chats.isEmpty()) return@runOnUiThread toast("لا توجد محادثات بعد، حاول بعد لحظات")
                    val checked = BooleanArray(chats.size) { chats[it].first in sources }
                    AlertDialog.Builder(this)
                        .setTitle("اختر المصادر")
                        .setMultiChoiceItems(chats.map { it.second }.toTypedArray(), checked) { _, i, c ->
                            if (c) sources.add(chats[i].first) else sources.remove(chats[i].first)
                        }
                        .setPositiveButton("تم") { _, _ -> refresh() }
                        .show()
                }
            }
        }
        dstBtn.setOnClickListener {
            TgClient.loadChats { chats ->
                runOnUiThread {
                    if (chats.isEmpty()) return@runOnUiThread toast("لا توجد محادثات بعد، حاول بعد لحظات")
                    AlertDialog.Builder(this)
                        .setTitle("اختر الوجهة")
                        .setSingleChoiceItems(chats.map { it.second }.toTypedArray(), chats.indexOfFirst { it.first == dest }) { d, i ->
                            dest = chats[i].first; refresh(); d.dismiss()
                        }
                        .show()
                }
            }
        }
        val inc = edit("كلمات يجب أن تحتويها الرسالة (مفصولة بفاصلة، اتركها فارغة للكل)", old?.include?.joinToString(",") ?: "")
        val exc = edit("كلمات تُستثنى (مفصولة بفاصلة)", old?.exclude?.joinToString(",") ?: "")
        val copy = CheckBox(this).apply {
            text = "إرسال كنسخة (بدون \"تم التحويل من\")"
            isChecked = old?.asCopy ?: false
            root.addView(this)
        }
        fun words(s: String) = s.split(",", "،", "\n").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        btn("حفظ") {
            if (sources.isEmpty() || dest == 0L) return@btn toast("اختر مصدرًا ووجهة")
            Store.upsert(
                Rule(
                    old?.id ?: UUID.randomUUID().toString(),
                    name.text.toString().ifBlank { "قاعدة" },
                    sources.toList(), dest,
                    words(inc.text.toString()), words(exc.text.toString()),
                    copy.isChecked, old?.enabled ?: true
                )
            )
            render()
        }
        btn("إلغاء") { render() }
    }
}
