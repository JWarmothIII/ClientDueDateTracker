# Screen Conventions

Every screen destination uses unidirectional data flow:

```text
owner API or use case -> ViewModel -> UiState -> content composable
                              ^                    |
                              +---- callbacks -----+
```

## Location and naming

Keep screen code with the owning or consuming feature:

```text
domain/<feature>/ui/
    <Feature>Screen.kt
    <Feature>UiState.kt
    <Feature>ViewModel.kt
```

The dashboard and onboarding screens are the current references.

## Responsibilities

`<Feature>Screen` is the stateful destination boundary. It obtains a Hilt ViewModel, collects state
with `collectAsStateWithLifecycle()`, and connects state/actions/navigation to a content
composable. It does not query Room or calculate deadlines.

`<Feature>Content` receives immutable state and callbacks. It renders loading, empty, error, and
data states and can be tested without Hilt. Small display formatting is allowed; product rules and
writes are not.

`<Feature>UiState` contains only renderer-ready immutable values. Use a sealed interface for
mutually exclusive loading/error/data states and a data class for independently editable form
fields. Never expose a DAO, entity, mutable flow, `Context`, or navigation controller.

`<Feature>ViewModel` depends on owner APIs or real coordinating use cases. It exposes immutable
`StateFlow`, launches work in `viewModelScope`, and handles verb-named actions such as `save`,
`complete`, `reopen`, or `refresh`. It never holds an Activity or composable state.

## IDs and navigation

Pass typed public IDs or their string values between destinations. Never navigate with Room
numeric keys, entities, repositories, or whole mutable objects. The destination loads current
state through an owner API.

## Forms

- Preserve user input in UI state while saving.
- Show definition loading and save progress separately.
- Validate required and cross-field business rules in the ViewModel/use case, not only through
  input keyboard settings.
- Display actionable validation errors without clearing the form.
- Disable duplicate submission while a save is active.
- Navigate only after the atomic operation reports success.

## Permissions

Explain why a platform permission is needed before invoking the system prompt. Persist that the
explanation was shown, respect denial, and provide a later settings recovery action. Permission
state is platform UI behavior and stays outside business models.

## Tests

- Unit-test ViewModel transformations and business-action handling.
- Compose-test content composables with explicit loading, empty, error, and data states.
- Exercise visible ordering and limits, form input and error presentation, and callbacks for quick
  actions.
- Add navigation or permission-host tests when those behaviors change.
- Use Room instrumentation tests for persistence invariants; a composable test is not evidence of
  an atomic database write.

## Review checklist

- State is collected lifecycle-aware at the destination boundary.
- Child composables receive values and callbacks, never a ViewModel.
- No composable accesses an API, repository, DAO, or database.
- ViewModels do not depend on DAOs or Room entities.
- All visible loading, empty, error, and data states are intentional.
- Business writes use the owning API or coordinating use case.
- Tests cover important state and callback behavior.
