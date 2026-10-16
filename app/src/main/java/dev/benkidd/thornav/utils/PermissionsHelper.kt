package dev.benkidd.thornav.utils

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import dev.benkidd.thornav.BuildConfig
import dev.benkidd.thornav.IUserService
import dev.benkidd.thornav.service.UserService
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class PermissionsHelper(private val context: Context) {
    private val fixMutex = Mutex()

    fun hasSecureSettingsPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun isShizukuAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        try {
            Shizuku.getVersion() >= 11
        } catch (_: Throwable) {
            false
        }
    }

    fun hasShizukuPermission(): Boolean = try {
        isShizukuAvailable() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    fun requestShizukuPermission(requestCode: Int) {
        if (isShizukuAvailable()) {
            Shizuku.requestPermission(requestCode)
        }
    }

    suspend fun grantPermissionViaShizuku(): Boolean = suspendCancellableCoroutine { continuation ->
        if (!hasShizukuPermission()) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        val componentName = ComponentName(BuildConfig.APPLICATION_ID, UserService::class.java.name)
        val args = Shizuku.UserServiceArgs(componentName)
            .daemon(false)
            .processNameSuffix("service")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                if (binder != null && binder.pingBinder()) {
                    try {
                        val userService = IUserService.Stub.asInterface(binder)
                        userService.grantPermission(
                            context.packageName,
                            Manifest.permission.WRITE_SECURE_SETTINGS,
                        )
                        if (continuation.isActive) {
                            continuation.resume(true)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error invoking UserService", e)
                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                    } finally {
                        try {
                            Shizuku.unbindUserService(args, this, true)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error unbinding UserService", e)
                        }
                    }
                } else {
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
        }

        continuation.invokeOnCancellation {
            try {
                Shizuku.unbindUserService(args, connection, true)
            } catch (_: Exception) {}
        }

        try {
            Shizuku.bindUserService(args, connection)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind UserService", e)
            if (continuation.isActive) {
                continuation.resume(false)
            }
        }
    }

    /**
     * Grant WRITE_SECURE_SETTINGS via root (one-time command).
     */
    fun grantPermissionViaRoot(): Boolean {
        val pkg = context.packageName
        return runSuCommand("pm grant $pkg android.permission.WRITE_SECURE_SETTINGS")
    }

    private fun runSuCommand(cmd: String): Boolean = try {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        val exitCode = process.waitFor()
        exitCode == 0
    } catch (e: Exception) {
        Log.e(TAG, "su command failed", e)
        false
    }

    /**
     * Toggles/re-writes the setting in Settings.Global.
     * OEM firmware observes "second_disable_back_gesture". When onChange fires,
     * SystemUI calls updateNavbarForSecondaryDisplay() which hides the secondary gesture bar.
     */
    suspend fun applyFix(): Boolean = fixMutex.withLock {
        withContext(Dispatchers.Main) {
            try {
                if (hasSecureSettingsPermission()) {
                    val cr = context.contentResolver
                    val current = Settings.Global.getInt(cr, KEY_SETTING, 0)
                    val temp = if (current == 1) 0 else 1

                    // Temporarily write opposite value to guarantee ContentObserver fires
                    Settings.Global.putInt(cr, KEY_SETTING, temp)
                    Log.d(TAG, "Toggled $KEY_SETTING to $temp")

                    delay(200.milliseconds)

                    return@withContext try {
                        Settings.Global.putInt(cr, KEY_SETTING, current)
                        Log.d(TAG, "Restored $KEY_SETTING to $current")
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error restoring setting", e)
                        false
                    }
                } else {
                    // If permission is missing, attempt to use root if available
                    return@withContext runSuCommand(
                        "settings put global $KEY_SETTING 1 && sleep 0.2 && settings put global $KEY_SETTING 0",
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error applying fix", e)
                return@withContext false
            }
        }
    }

    companion object {
        private const val TAG = "PermissionsHelper"
        private const val KEY_SETTING = "second_disable_back_gesture"
    }
}
