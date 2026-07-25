# Screen Conventions

Use this pattern for every screen-level navigation destination. It keeps screen state and
business behavior out of rendering composables while leaving smaller UI components easy to
preview, test, and reuse.

The project uses unidirectional data flow:

```text
repository or use case -> ViewModel -> UiState -> composables
                              ^                       |
                              +------ callbacks ------+
```

State flows down from the `ViewModel`. User intent flows up through callbacks. A composable
does not read from a repository, DAO, or other data source.

## Files and naming

Keep a feature's screen-level UI files together:

```text
domain/<feature>/ui/
    <Feature>Screen.kt
    <Feature>UiState.kt
    <Feature>ViewModel.kt
```

For example, the dashboard uses:

```text
domain/dashboard/ui/
    DashboardScreen.kt
    DashboardUiState.kt
    DashboardViewModel.kt
```

Add separate files for reusable components only when the screen file becomes difficult to
navigate. Do not create layers or interfaces that the feature does not yet need.

## Responsibilities

### `<Feature>Screen`

The public screen composable is the stateful boundary for a navigation destination. It may:

- Obtain the screen's `ViewModel` with `hiltViewModel()`.
- Collect `uiState` with `collectAsStateWithLifecycle()`.
- Connect ViewModel methods and navigation callbacks to the rendering composable.
- Own simple UI behavior such as a snackbar host, focus, scrolling, or a permission prompt.

It must not query a DAO or repository, calculate business rules, or pass the `ViewModel` deeper
into the composable tree.

### `<Feature>Content`

The content composable renders the screen. It:

- Receives immutable values and event callbacks as parameters.
- Produces the same UI for the same parameter values.
- Contains only display and UI behavior logic.
- Can be called from a preview or Compose test without Hilt or a `ViewModel`.

Small UI transformations are allowed here, such as choosing an icon for a visible status or
formatting already-prepared display data. Product decisions and data changes belong outside the
composable.

### `<Feature>UiState`

`UiState` is an immutable snapshot containing everything required to render the screen.

- Prefer a `data class` when fields can vary independently.
- Prefer a `sealed interface` when states such as loading, error, and content are mutually
  exclusive.
- Provide a safe initial state so the first frame is valid.
- Use read-only collections and renderer-ready values.
- Do not put a repository, DAO, `Flow`, mutable collection, `Context`, or navigation controller
  in UI state.
- Avoid exposing persistence entities when the UI needs a different representation. Map them to
  a UI or domain model in the ViewModel or a use case.

Transient element state that has no business meaning, such as whether a menu is expanded, should
stay near the composable that owns it with `remember` or `rememberSaveable`.

### `<Feature>ViewModel`

The ViewModel is the screen-level state holder. It:

- Depends on repositories or use cases, never directly on a DAO or database.
- Transforms data-layer streams into a public `StateFlow<FeatureUiState>`.
- Uses `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState)` for
  observable data streams.
- Handles user actions that require business logic through verb-named methods such as
  `saveClient()`, `completeRequirement()`, or `refresh()`.
- Launches asynchronous actions in `viewModelScope`.
- Does not hold an `Activity`, `Context`, Compose state, UI component, or navigation controller.

If business logic is reused by multiple ViewModels or makes a ViewModel difficult to understand,
move it into a use case. Do not create a use case only to forward one repository call.

## Events and navigation

Pass event handlers down as lambdas:

```kotlin
ClientListContent(
    uiState = uiState,
    onClientClick = onClientClick,
    onArchiveClient = viewModel::archiveClient,
)
```

- UI-only events, including navigation, are handled by the screen or navigation layer.
- Events that change application data are delegated to the ViewModel.
- Reusable child composables receive only the state they display and callbacks they can invoke.
- Do not pass a `ViewModel` into a child composable.
- Prefer representing ViewModel outcomes as state instead of sending one-time events through a
  `Channel` or `SharedFlow`. If the UI consumes a transient message, expose it in state and report
  its dismissal back to the ViewModel.

Navigation destinations should pass small stable identifiers, such as a client ID. The
destination ViewModel loads the corresponding data instead of receiving an entire object through
navigation.

## Reusable template

The following skeleton is the starting point for a new screen. Replace `ClientList` with the
feature name and add only the state and callbacks the screen needs.

`ClientListUiState.kt`:

```kotlin
package dev.jwarmothiii.clientduedatetracker.domain.clientlist.ui

sealed interface ClientListUiState {
    data object Loading : ClientListUiState

    data object LoadFailed : ClientListUiState

    data class Success(
        val clients: List<ClientListItemUiState>,
    ) : ClientListUiState
}

data class ClientListItemUiState(
    val id: Long,
    val name: String,
    val dueDateLabel: String,
)
```

`ClientListViewModel.kt`:

```kotlin
package dev.jwarmothiii.clientduedatetracker.domain.clientlist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jwarmothiii.clientduedatetracker.data.repository.ClientRepository
import dev.jwarmothiii.clientduedatetracker.domain.client.model.Client
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ClientListViewModel
    @Inject
    constructor(
        clientRepository: ClientRepository,
    ) : ViewModel() {
        val uiState: StateFlow<ClientListUiState> =
            clientRepository
                .observeClients()
                .map(::toClientListUiState)
                .catch {
                    emit(ClientListUiState.LoadFailed)
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = ClientListUiState.Loading,
                )
    }

private fun toClientListUiState(clients: List<Client>): ClientListUiState =
    ClientListUiState.Success(
        clients =
            clients.map { client ->
                ClientListItemUiState(
                    id = client.id,
                    name = client.name,
                    dueDateLabel = client.dueDate.toString(),
                )
            },
    )
```

`ClientListScreen.kt`:

```kotlin
package dev.jwarmothiii.clientduedatetracker.domain.clientlist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ClientListScreen(
    onClientClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClientListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ClientListContent(
        uiState = uiState,
        onClientClick = onClientClick,
        modifier = modifier,
    )
}

@Composable
private fun ClientListContent(
    uiState: ClientListUiState,
    onClientClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Render loading, error, empty, and populated states here.
}
```

The template shows the dependency direction, not required boilerplate. A static screen does not
need a ViewModel until it owns screen state or business actions.

## Previews and tests

- Preview the content composable with representative loading, empty, error, and populated states.
- Unit test the ViewModel's state transformations and business-action methods.
- Compose test the content composable with explicit state and callbacks.
- Add a navigation test when a destination or route argument is introduced.
- Prefer test repositories or other small fakes over tests that only verify mock calls.

## Review checklist

Before opening a pull request for a screen, confirm:

- The destination has `Screen`, `UiState`, and `ViewModel` files when it owns business state.
- The ViewModel exposes immutable state and does not expose mutable flows.
- Flow state is collected lifecycle-aware at the screen boundary.
- Rendering composables accept values and callbacks, not a ViewModel.
- Composables do not access repositories, DAOs, or databases.
- Navigation stays in the UI/navigation layer.
- The content composable has useful previews and can be tested without dependency injection.
- ViewModel behavior has unit coverage when it transforms data or handles actions.

## Basis for this convention

This project convention adapts, rather than duplicates, patterns used by the Android team:

- [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [Android UI layer guidance](https://developer.android.com/topic/architecture/ui-layer)
- [Android state holders and UI state guidance](https://developer.android.com/topic/architecture/ui-layer/stateholders)
- [Android UI events guidance](https://developer.android.com/topic/architecture/ui-layer/events)
- [Now in Android architecture](https://github.com/android/nowinandroid/blob/main/docs/ArchitectureLearningJourney.md)
- [Android architecture samples](https://github.com/android/architecture-samples)
- [Official Jetpack Compose samples](https://github.com/android/compose-samples)
