package dev.jwarmothiii.clientduedatetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import dagger.hilt.android.AndroidEntryPoint
import dev.jwarmothiii.clientduedatetracker.app.navigation.ClientDueDateTrackerNavigation
import dev.jwarmothiii.clientduedatetracker.designsystem.theme.ClientDueDateTrackerTheme
import dev.jwarmothiii.clientduedatetracker.platform.notification.NotificationPermissionHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClientDueDateTrackerTheme {
                Surface {
                    NotificationPermissionHost { notificationsDenied, openSettings ->
                        ClientDueDateTrackerNavigation(
                            notificationsDenied = notificationsDenied,
                            onOpenNotificationSettings = openSettings,
                        )
                    }
                }
            }
        }
    }
}
