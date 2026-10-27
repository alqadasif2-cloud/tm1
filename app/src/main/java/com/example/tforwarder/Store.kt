package com.example.tforwarder

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Rule(
    val id: String,
    val name: String,
    val sources: List<Long>,
    val dest: Long,
    val include: List<String>,
    val exclude: List<String>,
    val asCopy: Boolean,
    val enabled: Boolean
)

object Store {
    private lateinit var p: SharedPreferences
    @Volatile private var cache: List<Rule>? = null

    fun init(c: Context) {
        if (!::p.isInitialized) p = c.getSharedPreferences("tf", Context.MODE_PRIVATE)
    }

    var apiId: Int
        get() = p.getInt("api_id", 0)
        set(v) { p.edit().putInt("api_id", v).apply() }

    var apiHash: String
        get() = p.getString("api_hash", "") ?: ""
        set(v) { p.edit().putString("api_hash", v).apply() }

    var running: Boolean
        get() = p.getBoolean("running", false)
        set(v) { p.edit().putBoolean("running", v).apply() }

    fun rules(): List<Rule> {
        cache?.let { return it }
        val out = mutableListOf<Rule>()
        val arr = JSONArray(p.getString("rules", "[]"))
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Rule(
                    o.getString("id"), o.getString("name"),
                    o.getJSONArray("sources").let { a -> (0 until a.length()).map { a.getLong(it) } },
                    o.getLong("dest"),
                    o.getJSONArray("include").let { a -> (0 until a.length()).map { a.getString(it) } },
                    o.getJSONArray("exclude").let { a -> (0 until a.length()).map { a.getString(it) } },
                    o.getBoolean("asCopy"), o.getBoolean("enabled")
                )
            )
        }
        cache = out
        return out
    }

    fun saveRules(list: List<Rule>) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id); put("name", r.name)
                put("sources", JSONArray(r.sources)); put("dest", r.dest)
                put("include", JSONArray(r.include)); put("exclude", JSONArray(r.exclude))
                put("asCopy", r.asCopy); put("enabled", r.enabled)
            })
        }
        p.edit().putString("rules", arr.toString()).apply()
        cache = list
    }

    fun upsert(r: Rule) {
        val l = rules().toMutableList()
        val i = l.indexOfFirst { it.id == r.id }
        if (i >= 0) l[i] = r else l.add(r)
        saveRules(l)
    }

    fun delete(id: String) = saveRules(rules().filter { it.id != id })

    private fun key() = "cnt_" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    fun inc() { p.edit().putInt(key(), p.getInt(key(), 0) + 1).apply() }
    fun today(): Int = p.getInt(key(), 0)
}
