package com.example.tforwarder

import android.content.Context
import android.os.Build
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap

object TgClient {
    private lateinit var ctx: Context
    private var client: Client? = null
    private val titles = ConcurrentHashMap<Long, String>()

    @Volatile var auth: TdApi.AuthorizationState? = null
    @Volatile var listener: (() -> Unit)? = null

    fun init(c: Context) { ctx = c.applicationContext }

    fun title(id: Long): String = if (id == 0L) "غير محدد" else (titles[id] ?: id.toString())

    @Synchronized
    fun start() {
        if (client != null || Store.apiId == 0) return
        Client.execute(TdApi.SetLogVerbosityLevel(1))
        client = Client.create({ o -> onUpdate(o) }, null, null)
    }

    fun send(f: TdApi.Function<*>, cb: ((TdApi.Object) -> Unit)? = null) {
        start()
        client?.send(f) { r -> cb?.invoke(r) }
    }

    private fun onUpdate(o: TdApi.Object) {
        when (o) {
            is TdApi.UpdateAuthorizationState -> handleAuth(o.authorizationState)
            is TdApi.UpdateNewChat -> titles[o.chat.id] = o.chat.title
            is TdApi.UpdateChatTitle -> titles[o.chatId] = o.title
            is TdApi.UpdateNewMessage -> Forwarder.handle(o.message)
        }
    }

    private fun handleAuth(s: TdApi.AuthorizationState) {
        auth = s
        when (s) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val p = TdApi.SetTdlibParameters()
                p.databaseDirectory = ctx.filesDir.absolutePath + "/td"
                p.useMessageDatabase = true
                p.useFileDatabase = true
                p.useChatInfoDatabase = true
                p.useSecretChats = false
                p.apiId = Store.apiId
                p.apiHash = Store.apiHash
                p.systemLanguageCode = "en"
                p.deviceModel = Build.MODEL
                p.systemVersion = Build.VERSION.RELEASE
                p.applicationVersion = "1.0"
                send(p)
            }
            is TdApi.AuthorizationStateReady -> {
                val l = TdApi.LoadChats()
                l.chatList = TdApi.ChatListMain()
                l.limit = 300
                send(l)
            }
            is TdApi.AuthorizationStateClosed -> {
                client = null
                auth = null
                start()
            }
            else -> {}
        }
        listener?.invoke()
    }

    /** يجلب قائمة المحادثات (id, الاسم). */
    fun loadChats(done: (List<Pair<Long, String>>) -> Unit) {
        val g = TdApi.GetChats()
        g.chatList = TdApi.ChatListMain()
        g.limit = 300
        send(g) { r ->
            if (r is TdApi.Chats) done(r.chatIds.map { it to title(it) }) else done(emptyList())
        }
    }
}
