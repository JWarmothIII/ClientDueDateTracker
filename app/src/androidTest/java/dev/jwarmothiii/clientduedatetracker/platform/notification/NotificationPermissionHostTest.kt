package dev.jwarmothiii.clientduedatetracker.platform.notification

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NotificationPermissionHostTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun clearExplanation() {
        ApplicationProvider
            .getApplicationContext<Context>()
            .getSharedPreferences("notification_onboarding", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun firstLaunchExplainsGenericNotificationBeforePrompting() {
        compose.setContent {
            NotificationPermissionHost { _, _ -> }
        }

        compose.onNodeWithText("Daily reminder summaries").assertIsDisplayed()
        compose
            .onNodeWithText(
                "Allow notifications to receive one generic daily summary when work is due or overdue. " +
                    "Notifications never include client initials, requirement names, dates, or notes.",
            ).assertIsDisplayed()
    }
}
