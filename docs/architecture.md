# Architecture

The Android app is organized by business domain, with supporting code grouped by responsibility.

```text
domain/     Business-focused areas and their UI, state, and models
data/       Persistence and repository implementations
platform/   Android platform integrations
shared/     Reusable UI, navigation, utilities, and theme
```

## UI layer

Each screen-level destination follows unidirectional data flow:

```text
repository or use case -> ViewModel -> UiState -> screen content
                              ^                    |
                              +---- callbacks -----+
```

Feature UI lives under `domain/<feature>/ui`. A screen ViewModel prepares immutable UI state and
handles business actions. The public screen composable connects that state and those actions to a
stateless content composable. Navigation and other UI behavior remain in the UI layer.

See [Screen Conventions](screen-conventions.md) for file naming, responsibility boundaries,
event handling, previews, testing guidance, and a reusable screen template.

## Data layer

Room database definitions, entities, DAOs, and database modules live in `data/database`.
Repository classes that coordinate persistence access live in `data/repository`, keeping direct
database access out of composable UI functions.

The first Room schema is `client_due_date_tracker.db` version 1. It contains only the temporary
`persistence_test_table` table used to prove Room persistence before the real app model is designed.
