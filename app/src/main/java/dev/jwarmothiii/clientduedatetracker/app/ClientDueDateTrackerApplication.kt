package dev.jwarmothiii.clientduedatetracker.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import dev.jwarmothiii.clientduedatetracker.platform.notification.DailyMaintenanceScheduler
import javax.inject.Inject

@HiltAndroidApp
class ClientDueDateTrackerApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun onCreate() {
        super.onCreate()
        DailyMaintenanceScheduler.ensureScheduled(this)
    }
}
