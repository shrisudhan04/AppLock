package com.strictfocus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val session = runCatching { runBlocking { StrictLockManager.sessionFlow(context).first() } }.getOrNull()
        if (session?.isActive == true) {
            // The accessibility service remains user-enabled across reboot on most devices.
            // The persisted absolute expiry is checked as soon as the service sees a package event.
        } else {
            runCatching { runBlocking { StrictLockManager.clear(context) } }
        }
    }
}
