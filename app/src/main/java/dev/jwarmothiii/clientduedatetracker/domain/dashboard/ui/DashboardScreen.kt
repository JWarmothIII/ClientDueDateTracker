@file:Suppress("ktlint:standard:function-naming")

package dev.jwarmothiii.clientduedatetracker.domain.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.DashboardRequirement
import dev.jwarmothiii.clientduedatetracker.domain.notes.api.DashboardNote
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    onAddClient: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    notificationsDenied: Boolean,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(
        state = state,
        onAddClient = onAddClient,
        onOpenNotificationSettings = onOpenNotificationSettings,
        notificationsDenied = notificationsDenied,
        onComplete = viewModel::complete,
        onReopen = viewModel::reopen,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
internal fun DashboardContent(
    state: DashboardUiState,
    onAddClient: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    notificationsDenied: Boolean,
    onComplete: (dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementId) -> Unit,
    onReopen: (dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementId) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Surface {
                Button(
                    onClick = onAddClient,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Add client")
                }
            }
        },
    ) { innerPadding ->
        when (state) {
            DashboardUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Text("Loading dashboard", modifier = Modifier.padding(top = 12.dp))
                }
            }

            is DashboardUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Retry")
                    }
                }
            }

            is DashboardUiState.Data -> {
                DashboardData(
                    state = state,
                    notificationsDenied = notificationsDenied,
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    onComplete = onComplete,
                    onReopen = onReopen,
                    contentPadding =
                        PaddingValues(
                            start = 24.dp,
                            top = 24.dp,
                            end = 24.dp,
                            bottom = innerPadding.calculateBottomPadding() + 20.dp,
                        ),
                )
            }
        }
    }
}

@Composable
private fun DashboardData(
    state: DashboardUiState.Data,
    notificationsDenied: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onComplete: (dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementId) -> Unit,
    onReopen: (dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementId) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${state.activeClientCount} active clients",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (notificationsDenied) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Notifications are off", style = MaterialTheme.typography.titleMedium)
                        Text("Turn them on in Android settings to receive daily reminder summaries.")
                        OutlinedButton(onClick = onOpenNotificationSettings) {
                            Text("Open settings")
                        }
                    }
                }
            }
        }
        requirementSection("Overdue", state.overdue, "No overdue requirements.", onComplete)
        requirementSection("Due in the next 7 days", state.dueSoon, "Nothing due soon.", onComplete)
        item { Text("Recently completed", style = MaterialTheme.typography.titleLarge) }
        val recent = state.recentlyCompleted.take(5)
        if (recent.isEmpty()) {
            item { EmptyCard("No recently completed requirements.") }
        } else {
            items(recent, key = { it.requirement.id.value }) { row ->
                RequirementCard(row = row, actionLabel = "Reopen", onAction = { onReopen(row.requirement.id) })
            }
        }
        item { Text("Pinned notes", style = MaterialTheme.typography.titleLarge) }
        val pinned = state.pinnedNotes.take(3)
        if (pinned.isEmpty()) {
            item { EmptyCard("No pinned notes.") }
        } else {
            items(pinned, key = { it.note.id.value }) { PinnedNoteCard(it) }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.requirementSection(
    title: String,
    rows: List<DashboardRequirement>,
    emptyText: String,
    onComplete: (dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementId) -> Unit,
) {
    item { Text(title, style = MaterialTheme.typography.titleLarge) }
    if (rows.isEmpty()) {
        item { EmptyCard(emptyText) }
    } else {
        items(rows, key = { it.requirement.id.value }) { row ->
            RequirementCard(row = row, actionLabel = "Complete", onAction = { onComplete(row.requirement.id) })
        }
    }
}

@Composable
private fun RequirementCard(
    row: DashboardRequirement,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(row.requirement.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${row.clientInitials} · ${row.requirement.dueDate.format(DATE_FORMAT)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun PinnedNoteCard(row: DashboardNote) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(row.note.content, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Text(
                row.clientInitials,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, uuuu")
