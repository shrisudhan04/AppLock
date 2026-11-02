package com.strictfocus

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class StrictFocusAccessibilityService : AccessibilityService() {

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var lastRedirectAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val pkg = event.packageName?.toString() ?: return

        if (
            pkg == packageName ||
            pkg == "com.android.systemui"
        ) {
            return
        }

        scope.launch {
            if (!StrictLockManager.isLocked(this@StrictFocusAccessibilityService, pkg)) {
                return@launch
            }

            val now = System.currentTimeMillis()

            if (now - lastRedirectAt < 800L) {
                return@launch
            }

            lastRedirectAt = now

            performGlobalAction(GLOBAL_ACTION_HOME)

            val intent = Intent(
                this@StrictFocusAccessibilityService,
                StrictBlockActivity::class.java
            ).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )

            runCatching {
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}