package dev.jwarmothiii.clientduedatetracker.platform.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jwarmothiii.clientduedatetracker.MainActivity
import dev.jwarmothiii.clientduedatetracker.R
import dev.jwarmothiii.clientduedatetracker.domain.notification.usecase.NotificationPoster
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidNotificationPoster
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : NotificationPoster {
        override fun postGenericSummary(requirementCount: Int): Boolean {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
            createChannel()
            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Client Due Date Tracker")
                    .setContentText(
                        if (requirementCount == 1) {
                            "You have an item that needs attention."
                        } else {
                            "You have items that need attention."
                        },
                    ).setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()
            return try {
                NotificationManagerCompat.from(context).notify(SUMMARY_NOTIFICATION_ID, notification)
                true
            } catch (_: SecurityException) {
                false
            }
        }

        private fun createChannel() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Daily reminders",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Generic daily summaries for due and overdue work."
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
                }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        private companion object {
            const val CHANNEL_ID = "daily_reminders"
            const val SUMMARY_NOTIFICATION_ID = 1001
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationPlatformModule {
    @Binds
    abstract fun bindNotificationPoster(implementation: AndroidNotificationPoster): NotificationPoster
}
