package com.music.bitchord.ui.classipod

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * Lets the Classipod PIP float over the native lock screen.
 *
 * Android hides every ordinary overlay window (`TYPE_APPLICATION_OVERLAY`)
 * while the keyguard is up, and no flag changes that for an app. An
 * accessibility service's own overlay windows (`TYPE_ACCESSIBILITY_OVERLAY`)
 * are the exception: they sit above the lock screen and the notification
 * shade. So while this service is switched on, [FloatingPodService] adds its
 * windows through it instead — the system lock screen stays exactly as it is,
 * with the PIP on top.
 *
 * The service reads nothing: it asks for no window content and takes no
 * events beyond the one it can't avoid declaring. It only lends its window
 * token. With it off, the PIP falls back to a normal overlay (everywhere but
 * the lock screen).
 */
class PodAccessibilityService : AccessibilityService() {

    companion object {
        /** The connected service, or null while it is switched off. */
        var instance: PodAccessibilityService? = null
            private set

        /** Told whenever [instance] changes, on the main thread. */
        internal var onAvailabilityChanged: (() -> Unit)? = null

        /** Opens the system Accessibility settings, where the service is switched on. */
        fun openSettings(context: Context) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        onAvailabilityChanged?.invoke()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        release()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release() {
        if (instance !== this) return
        instance = null
        onAvailabilityChanged?.invoke()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
