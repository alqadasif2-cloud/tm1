package com.example.tforwarder

import org.drinkless.tdlib.TdApi

object Forwarder {
    private fun textOf(c: TdApi.MessageContent): String = when (c) {
        is TdApi.MessageText -> c.text.text
        is TdApi.MessagePhoto -> c.caption.text
        is TdApi.MessageVideo -> c.caption.text
        is TdApi.MessageDocument -> c.caption.text
        is TdApi.MessageAudio -> c.caption.text
        is TdApi.MessageAnimation -> c.caption.text
        is TdApi.MessageVoiceNote -> c.caption.text
        else -> ""
    }

    fun handle(m: TdApi.Message) {
        if (!Store.running) return
        val text = textOf(m.content).lowercase()
        for (r in Store.rules()) {
            if (!r.enabled || m.chatId !in r.sources || m.chatId == r.dest) continue
            if (r.include.isNotEmpty() && r.include.none { text.contains(it) }) continue
            if (r.exclude.any { text.contains(it) }) continue
            val f = TdApi.ForwardMessages()
            f.chatId = r.dest
            f.fromChatId = m.chatId
            f.messageIds = longArrayOf(m.id)
            f.sendCopy = r.asCopy
            f.removeCaption = false
            TgClient.send(f) { res -> if (res is TdApi.Messages) Store.inc() }
        }
    }
}
