@file:Suppress("ktlint:standard:function-naming")

package dev.jwarmothiii.clientduedatetracker.platform.notification

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun NotificationPermissionHost(content: @Composable (notificationsDenied: Boolean, openSettings: () -> Unit) -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE) }
    var explained by remember { mutableStateOf(preferences.getBoolean(KEY_EXPLAINED, false)) }
    var granted by remember { mutableStateOf(context.notificationsGranted()) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            granted = permissionGranted
        }
    val requestPermission = {
        explained = true
        preferences.edit().putBoolean(KEY_EXPLAINED, true).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            granted = true
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) granted = context.notificationsGranted()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (!explained) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Daily reminder summaries") },
            text = {
                Text(
                    "Allow notifications to receive one generic daily summary when work is due or overdue. " +
                        "Notifications never include client initials, requirement names, dates, or notes.",
                )
            },
            confirmButton = {
                TextButton(onClick = requestPermission) { Text("Continue") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        explained = true
                        preferences.edit().putBoolean(KEY_EXPLAINED, true).apply()
                    },
                ) {
                    Text("Not now")
                }
            },
        )
    }
    content(
        explained && !granted,
        {
            val intent =
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
            (context as? Activity)?.startActivity(intent) ?: context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
    )
}

private fun Context.notificationsGranted(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private const val PREFERENCES_NAME = "notification_onboarding"
private const val KEY_EXPLAINED = "explanation_shown"
