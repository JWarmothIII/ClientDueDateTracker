# Architecture

Client Due Date Tracker is one Android application module organized by business ownership.

```text
app/                       application setup and navigation
database/                  Room database and transaction runner
designsystem/              theme and reusable visual tokens
platform/notification/     Android permission, notification, and WorkManager integration
domain/
  client/                  clients and atomic onboarding
  contractdefinition/      contract types, templates, policies, and seed configuration
  contracttracking/        contracts, events, occurrences, and generation
  notes/                   client-owned notes
  dashboard/               read-only composition plus quick requirement actions
  notification/            reminder selection and delivery history
```

The write-owning domains are `client`, `contractdefinition`, `contracttracking`, and `notes`.
Dashboard and notification consume owner APIs and do not own business records.

## Boundary rules

Each domain uses only the roles it needs:

```text
api/       stable cross-domain commands and read models
model/     framework-free business types and rules
data/      Room access, mappings, and API implementations
ui/        ViewModels, UI state, and Compose destinations
usecase/   real multi-step or cross-domain workflows
```

- A domain accesses another domain through its `api` package.
- A domain must not import another domain's repository implementation, DAO, mapper, or UI type.
- Business models do not depend on Android, Compose, Room, or Hilt.
- Public IDs are validated, nonblank strings. Repository implementations generate UUID strings;
  Room uses internal `Long` primary and foreign keys.
- Calendar values use `LocalDate` and are stored as epoch days. Moments use UTC `Instant` and are
  stored as epoch milliseconds.
- Writes establishing one invariant run through `DatabaseTransactionRunner`.
- Architecture tests enforce the import rules, framework-free models, and the absence of generic
  `shared` or `core` packages.

## Persistence

`ClientDueDateDatabase` version 1 is the canonical business schema. There are no migrations from
the former persistence proof; development installations containing it must be cleared or
reinstalled.

The schema contains:

- `clients`
- `contract_types`
- `requirement_templates`
- `contracts`
- `contract_definition_assignments`
- `contract_events`
- `requirements`
- `requirement_suppressions`
- `notes`
- `notification_deliveries`

The exported schema is under
`app/schemas/dev.jwarmothiii.clientduedatetracker.database.ClientDueDateDatabase/1.json`. See
[V1 ERD](erd.md) for ownership, foreign keys, and deletion behavior.

Deadline policies are stored as a versioned JSON DTO with stable enum codes. Unknown versions,
missing fields, corrupt values, and unknown codes throw `DataIntegrityException`; the app never
silently calculates from corrupt configuration.

## Definition and occurrence lifecycle

CTS, USPO, and State / RSUD definitions are seeded as database configuration. Templates snapshot
their title, instructions, calculated due date, and notification lead into every generated
requirement.

Used contract types and templates are replaced with a new lineage version. Future-only replacement
leaves existing assignments on the old version while preventing new selection. Future-and-active
replacement migrates active assignments; existing occurrences remain unchanged. Explicit
deactivation removes assignments so future generation stops without deleting pending or completed
history.

The generator materializes through today plus 30 days. Occurrence keys make reruns idempotent.
Deleting a generated occurrence records its key in `requirement_suppressions`, preventing
recreation. Recalculation changes or removes only pending, non-customized generated work; completed
and manually customized occurrences remain unchanged.

## User flows

Onboarding creates a validated client, its one contract, definition assignments, and initial
occurrences in one Room transaction. The dashboard combines owner read APIs to show:

- active-client count;
- all overdue requirements, oldest first;
- requirements due today through seven days ahead;
- five most recently completed requirements;
- three most recently updated pinned notes.

Requirement rows support completion and reopening. Reopening clears the completion timestamp.
Pinned notes are display-only on the dashboard.

## Background work and privacy

One unique one-time WorkManager chain schedules itself for approximately 10:00 AM device-local
time. A run generates missing occurrences, selects unsent lead and daily overdue reminders, posts
one generic summary, and records delivery only after Android accepts the post. Generation failures
return `Result.retry()`.

The first launch explains notification content before requesting Android 13+ permission. Denial is
respected; the dashboard provides a link to Android application settings.

Notification text never contains initials, requirement titles, dates, notes, or other
client-specific details. The app has no application server or analytics upload, but Android backup
and device transfer remain enabled. Free-form notes may contain sensitive information, so the app
does not claim that local data is de-identified.

## UI flow

Screen destinations use unidirectional data flow:

```text
owner API or use case -> ViewModel -> UiState -> content composable
                              ^                    |
                              +---- callbacks -----+
```

See [Screen Conventions](screen-conventions.md) for the project pattern.
