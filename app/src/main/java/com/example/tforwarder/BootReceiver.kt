package com.example.tforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Store.init(c)
        if (i.action == Intent.ACTION_BOOT_COMPLETED && Store.running) {
            try {
                ContextCompat.startForegroundService(c, Intent(c, ForwardService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
