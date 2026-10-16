package dev.benkidd.thornav.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dev.benkidd.thornav.R
import dev.benkidd.thornav.ThorFixService
import dev.benkidd.thornav.ui.theme.ThorNavFixTheme
import dev.benkidd.thornav.utils.PermissionsHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private var refreshTrigger by mutableIntStateOf(0)
    private val viewModel by viewModel<MainViewModel>()
    private val permissionsHelper: PermissionsHelper by inject()

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    performShizukuGrant()
                } else {
                    Toast.makeText(this, getString(R.string.msg_shizuku_denied), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)

        setContent {
            ThorNavFixTheme {
                MainScreen(
                    viewModel = viewModel,
                    refreshTrigger = refreshTrigger,
                    onRefresh = { refreshTrigger++ },
                    hasSecureSettingsPermission = permissionsHelper::hasSecureSettingsPermission,
                    onRequestShizukuPermission = {
                        try {
                            permissionsHelper.requestShizukuPermission(SHIZUKU_CODE)
                        } catch (e: Throwable) {
                            Toast.makeText(
                                this,
                                getString(
                                    R.string.msg_shizuku_request_failed,
                                    e.message ?: getString(R.string.unknown_error),
                                ),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    grantPermissionViaRoot = permissionsHelper::grantPermissionViaRoot,
                    onPerformShizukuGrant = { performShizukuGrant() },
                    isShizukuAvailable = permissionsHelper::isShizukuAvailable,
                    hasShizukuPermission = permissionsHelper::hasShizukuPermission,
                    applyFix = permissionsHelper::applyFix,
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
    }

    override fun onResume() {
        super.onResume()
        refreshTrigger++
    }

    private fun performShizukuGrant() {
        lifecycleScope.launch {
            val success = permissionsHelper.grantPermissionViaShizuku()
            if (success) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.msg_shizuku_granted),
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.msg_shizuku_failed),
                    Toast.LENGTH_SHORT,
                ).show()
            }
            refreshTrigger++
        }
    }

    companion object {
        private const val SHIZUKU_CODE = 1001
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    refreshTrigger: Int,
    onRefresh: () -> Unit,
    hasSecureSettingsPermission: () -> Boolean,
    isShizukuAvailable: () -> Boolean,
    hasShizukuPermission: () -> Boolean,
    onRequestShizukuPermission: () -> Unit,
    grantPermissionViaRoot: () -> Boolean,
    onPerformShizukuGrant: () -> Unit,
    applyFix: suspend () -> Boolean,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val hasPermission = remember(refreshTrigger) {
        hasSecureSettingsPermission()
    }
    val isServiceActive = remember(refreshTrigger) {
        isAccessibilityServiceEnabled(context, ThorFixService::class.java)
    }

    var isApplyingFix by remember { mutableStateOf(false) }
    var showRootDialog by remember { mutableStateOf(false) }
    var showShizukuDialog by remember { mutableStateOf(false) }

    val screenOnEnabled by viewModel.isScreenOnEnabled.collectAsStateWithLifecycle()
    val userUnlockEnabled by viewModel.isUserUnlockEnabled.collectAsStateWithLifecycle()
    val bootEnabled by viewModel.isBootEnabled.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    fun handleShizukuClick() {
        if (!isShizukuAvailable()) {
            showShizukuDialog = true
            return
        }

        if (hasShizukuPermission()) {
            onPerformShizukuGrant()
        } else {
            onRequestShizukuPermission()
        }
    }

    if (showRootDialog) {
        AlertDialog(
            modifier = Modifier.fillMaxWidth(fraction = 0.5f),
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
            ),
            onDismissRequest = { showRootDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_root_title),
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_root_message, context.packageName),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRootDialog = false
                        val granted = grantPermissionViaRoot()
                        if (granted) {
                            Toast.makeText(
                                context,
                                R.string.msg_root_granted,
                                Toast.LENGTH_LONG,
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                R.string.msg_root_failed,
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                        onRefresh()
                    },
                ) {
                    Text(stringResource(R.string.dialog_root_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRootDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showShizukuDialog) {
        AlertDialog(
            onDismissRequest = { showShizukuDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_shizuku_title),
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_shizuku_message),
                )
            },
            confirmButton = {
                TextButton(onClick = { showShizukuDialog = false }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.app_description),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // Status Card
            SectionCard {
                Text(
                    text = stringResource(R.string.section_status_and_permissions),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Permission status row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.label_write_secure_settings),
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(
                            if (hasPermission) {
                                R.string.status_granted
                            } else {
                                R.string.status_not_granted
                            },
                        ),
                        fontWeight = FontWeight.Bold,
                        color = if (hasPermission) Color(0xFF4CAF50) else Color(0xFFF44336),
                    )
                }

                // Permission grant options
                if (!hasPermission) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilledTonalButton(
                            onClick = { handleShizukuClick() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.btn_grant_shizuku))
                        }

                        OutlinedButton(
                            onClick = { showRootDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.btn_grant_root))
                        }

                        TextButton(
                            onClick = {
                                val cmd = resources.getString(
                                    R.string.adb_grant_command,
                                    context.packageName,
                                )

                                scope.launch {
                                    clipboard.setClipEntry(
                                        ClipData.newPlainText(
                                            resources.getString(R.string.adb_command_clip_label),
                                            cmd,
                                        ).toClipEntry(),
                                    )

                                    Toast.makeText(
                                        context,
                                        R.string.msg_adb_copied,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.btn_copy_adb))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Accessibility service status row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.label_wake_watcher_service),
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(
                            if (isServiceActive) {
                                R.string.status_running
                            } else {
                                R.string.status_disabled
                            },
                        ),
                        fontWeight = FontWeight.Bold,
                        color = if (isServiceActive) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FilledTonalButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (isServiceActive) {
                                R.string.btn_accessibility_settings
                            } else {
                                R.string.btn_enable_accessibility
                            },
                        ),
                    )
                }
            }

            // Quick Actions Card
            SectionCard {
                Text(
                    text = stringResource(R.string.section_quick_actions),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = stringResource(R.string.desc_quick_actions),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        isApplyingFix = true

                        scope.launch {
                            val success = applyFix()
                            isApplyingFix = false

                            withContext(AndroidUiDispatcher.Main) {
                                Toast.makeText(
                                    context,
                                    if (success) {
                                        R.string.msg_fix_success
                                    } else {
                                        R.string.msg_fix_failed
                                    },

                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    },
                    enabled = !isApplyingFix,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.btn_apply_fix_now))
                }
            }

            // Automation Triggers Card
            SectionCard {
                Text(
                    text = stringResource(R.string.section_automation_triggers),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(8.dp))

                SwitchRow(
                    title = stringResource(R.string.trigger_screen_wake),
                    checked = screenOnEnabled,
                    onCheckedChange = viewModel::setScreenOnEnabled,
                )

                SwitchRow(
                    title = stringResource(R.string.trigger_device_unlock),
                    checked = userUnlockEnabled,
                    onCheckedChange = viewModel::setUserUnlockEnabled,
                )

                SwitchRow(
                    title = stringResource(R.string.trigger_device_boot),
                    checked = bootEnabled,
                    onCheckedChange = viewModel::setBootEnabled,
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content,
        )
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}

private fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
    val expectedComponentName = "${context.packageName}/${serviceClass.name}"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false

    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServices)
    while (colonSplitter.hasNext()) {
        val componentName = colonSplitter.next()
        if (componentName.equals(expectedComponentName, ignoreCase = true)) {
            return true
        }
    }
    return false
}
