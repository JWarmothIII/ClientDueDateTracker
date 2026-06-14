package dev.jwarmothiii.clientduedatetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import dev.jwarmothiii.clientduedatetracker.domain.dashboard.ui.DashboardScreen
import dev.jwarmothiii.clientduedatetracker.shared.theme.ClientDueDateTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClientDueDateTrackerTheme {
                Surface {
                    DashboardScreen()
                }
            }
        }
    }
}
