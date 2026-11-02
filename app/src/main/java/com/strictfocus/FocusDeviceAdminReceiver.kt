package com.strictfocus

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.runBlocking

class FocusDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        val mins = runCatching {
            runBlocking { (StrictLockManager.remainingMillis(context) / 60_000L).toString() }
        }.getOrDefault("unknown")
        return "A focus session is active. Approximately $mins minutes remain. Disabling Device Admin does not erase the saved timer."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Device Admin disabled.", Toast.LENGTH_SHORT).show()
    }
}
