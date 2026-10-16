package dev.benkidd.thornav

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import dev.benkidd.thornav.data.UserPreferences
import dev.benkidd.thornav.utils.PermissionsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

@SuppressLint("AccessibilityPolicy")
class ThorFixService : AccessibilityService(), CoroutineScope {
    private val userPreferences: UserPreferences by inject()
    private val permissionsHelper: PermissionsHelper by inject()
    private val job = SupervisorJob()

    override val coroutineContext = Dispatchers.Main + job

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    launch {
                        val screenOnEnabled = userPreferences.isScreenOnEnabled.first()
                        if (screenOnEnabled) {
                            Log.d(TAG, "Screen turned on, applying nav bar fix")
                            permissionsHelper.applyFix()
                        }
                    }
                }

                Intent.ACTION_USER_PRESENT -> {
                    launch {
                        val unlockEnabled = userPreferences.isUserUnlockEnabled.first()
                        if (unlockEnabled) {
                            Log.d(TAG, "User present / unlocked, applying nav bar fix")
                            permissionsHelper.applyFix()
                        }
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        Log.d(TAG, "ThorFixService connected")

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )

        // Run once on service connection
        launch {
            permissionsHelper.applyFix()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No event interception needed, we only use the service for its persistent background lifecycle
    }

    override fun onInterrupt() {
        Log.d(TAG, "ThorFixService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        isRunning = false
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        Log.d(TAG, "ThorFixService destroyed")
    }

    companion object {
        private const val TAG = "ThorFixService"
        var isRunning: Boolean = false
            private set
    }
}
