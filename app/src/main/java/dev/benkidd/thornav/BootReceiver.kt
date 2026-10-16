package dev.benkidd.thornav

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.benkidd.thornav.data.UserPreferences
import dev.benkidd.thornav.utils.PermissionsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootReceiver : BroadcastReceiver(), KoinComponent {
    private val userPreferences: UserPreferences by inject()
    private val permissionsHelper: PermissionsHelper by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            Log.d(TAG, "Boot completed received")

            goAsync {
                val bootEnabled = userPreferences.isBootEnabled.first()
                if (bootEnabled) {
                    permissionsHelper.applyFix()
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}

fun BroadcastReceiver.goAsync(
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    block: suspend () -> Unit,
) {
    val pendingResult = goAsync()
    scope.launch {
        try {
            block()
        } catch (e: Exception) {
            Log.e("BootReceiver", "Error in goAsync", e)
        } finally {
            pendingResult.finish()
        }
    }
}
