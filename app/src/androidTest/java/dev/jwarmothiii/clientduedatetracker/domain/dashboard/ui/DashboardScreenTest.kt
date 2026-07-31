package dev.jwarmothiii.clientduedatetracker.domain.dashboard.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.jwarmothiii.clientduedatetracker.domain.client.model.ClientId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.DashboardRequirement
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.ContractId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.Requirement
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementOrigin
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementStatus
import dev.jwarmothiii.clientduedatetracker.domain.notes.api.DashboardNote
import dev.jwarmothiii.clientduedatetracker.domain.notes.model.Note
import dev.jwarmothiii.clientduedatetracker.domain.notes.model.NoteId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DashboardScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun loadingErrorAndEmptyStatesAreVisible() {
        val state = androidx.compose.runtime.mutableStateOf<DashboardUiState>(DashboardUiState.Loading)
        compose.setContent {
            DashboardContent(
                state = state.value,
                onAddClient = {},
                onOpenNotificationSettings = {},
                notificationsDenied = false,
                onComplete = {},
                onReopen = {},
                onRetry = {},
            )
        }
        compose.onNodeWithText("Loading dashboard").assertIsDisplayed()

        compose.runOnIdle {
            state.value = DashboardUiState.Error("Database unavailable")
        }
        compose.onNodeWithText("Database unavailable").assertIsDisplayed()

        compose.runOnIdle {
            state.value = DashboardUiState.Data(0, emptyList(), emptyList(), emptyList(), emptyList())
        }
        compose.onNodeWithText("No overdue requirements.").assertIsDisplayed()
        compose.onNodeWithText("Nothing due soon.").assertIsDisplayed()
    }

    @Test
    fun quickCompleteAndReopenCallbacksReceiveTheSelectedRequirement() {
        val pending = requirement("pending", RequirementStatus.PENDING)
        val completed = requirement("completed", RequirementStatus.COMPLETED)
        var completedId: RequirementId? = null
        var reopenedId: RequirementId? = null
        compose.setContent {
            DashboardContent(
                state =
                    DashboardUiState.Data(
                        activeClientCount = 1,
                        overdue = listOf(pending),
                        dueSoon = emptyList(),
                        recentlyCompleted = listOf(completed),
                        pinnedNotes = emptyList(),
                    ),
                onAddClient = {},
                onOpenNotificationSettings = {},
                notificationsDenied = false,
                onComplete = { completedId = it },
                onReopen = { reopenedId = it },
                onRetry = {},
            )
        }

        compose.onNodeWithText("Complete").performClick()
        compose.onNodeWithText("Reopen").performClick()

        assertEquals(pending.requirement.id, completedId)
        assertEquals(completed.requirement.id, reopenedId)
    }

    @Test
    fun dashboardLimitsPinnedNotesToThreeAndShowsSettingsRecovery() {
        val notes = (1..4).map(::note)
        var settingsOpened = false
        compose.setContent {
            DashboardContent(
                state = DashboardUiState.Data(1, emptyList(), emptyList(), emptyList(), notes),
                onAddClient = {},
                onOpenNotificationSettings = { settingsOpened = true },
                notificationsDenied = true,
                onComplete = {},
                onReopen = {},
                onRetry = {},
            )
        }

        compose.onNodeWithText("Note 1").assertIsDisplayed()
        compose.onNodeWithText("Note 3").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("Note 4").fetchSemanticsNodes().size)
        compose.onNodeWithText("Open settings").performClick()
        assert(settingsOpened)
    }

    @androidx.compose.runtime.Composable
    private fun dataContent(state: DashboardUiState.Data) {
        DashboardContent(
            state = state,
            onAddClient = {},
            onOpenNotificationSettings = {},
            notificationsDenied = false,
            onComplete = {},
            onReopen = {},
            onRetry = {},
        )
    }

    private fun requirement(
        id: String,
        status: RequirementStatus,
    ): DashboardRequirement =
        DashboardRequirement(
            requirement =
                Requirement(
                    id = RequirementId(id),
                    contractId = ContractId("contract"),
                    templateId = null,
                    origin = RequirementOrigin.AD_HOC,
                    occurrenceKey = null,
                    title = id,
                    instructions = "",
                    dueDate = LocalDate.parse("2026-07-01"),
                    notificationLeadDays = 3,
                    status = status,
                    completedAt = if (status == RequirementStatus.COMPLETED) Instant.EPOCH else null,
                    manuallyCustomized = true,
                ),
            clientInitials = "AB",
        )

    private fun note(index: Int): DashboardNote =
        DashboardNote(
            note =
                Note(
                    id = NoteId("note-$index"),
                    clientId = ClientId("client"),
                    requirementId = null,
                    content = "Note $index",
                    pinned = true,
                    createdAt = Instant.EPOCH,
                    updatedAt = Instant.EPOCH.plusSeconds(index.toLong()),
                ),
            clientInitials = "AB",
        )
}
