# Core Android Dependency Guide

This guide explains the dependencies requested by this task and how they support the
Client Due Date Tracker.

The acceptance criteria lists Compose, Room, and WorkManager more than once. These are
duplicate requirements rather than separate dependencies. Compose and Jetpack Compose
refer to the same UI toolkit.

## Dependency Summary

| Dependency | Purpose in this app |
| --- | --- |
| Jetpack Compose | Builds screens and reusable UI using Kotlin |
| ViewModel for Compose | Stores screen state and business logic across recompositions and configuration changes |
| Navigation Compose | Moves between Compose screens and passes route arguments |
| Room | Stores clients, contracts, requirements, due dates, and notes locally |
| WorkManager | Runs reliable background work, such as checking due dates and scheduling notifications |
| Hilt | Creates and supplies shared objects such as the database, repositories, and ViewModels |
## Jetpack Compose

Jetpack Compose is Android's Kotlin-based UI toolkit. Instead of defining screens in XML,
the app describes its UI with composable Kotlin functions.

Compose is used for:

- Screens such as the dashboard and client details
- Reusable controls, cards, lists, dialogs, and forms
- Material 3 styling and theming
- Updating the visible UI when ViewModel state changes

The Compose BOM controls versions only for Compose libraries. It does not control versions
for Room, WorkManager, Navigation, Lifecycle, or Hilt.

## ViewModel for Compose

A ViewModel holds screen state and coordinates actions that should not live directly in
the UI. It survives configuration changes, such as device rotation, and prevents state
from being recreated during every Compose recomposition.

In this app, ViewModels can:

- Load clients and requirements from repositories
- Calculate or request due-date information
- Expose dashboard state to composable screens
- Handle user actions such as saving a client or completing a requirement

The Compose integration provides APIs such as `viewModel()` for obtaining a ViewModel from
a composable. When Hilt is used, navigation destinations commonly obtain ViewModels with
`hiltViewModel()` from the Hilt Navigation Compose integration.

## Navigation Compose

Navigation Compose manages movement between composable screens. It provides a navigation
host, routes, back-stack behavior, and support for route arguments.

Likely destinations in this app include:

- Dashboard
- Client list
- Client details
- Requirement details
- Add or edit forms

Navigation arguments should usually contain small identifiers, such as a client ID.
The destination's ViewModel can use that ID to load the complete record from Room instead
of passing an entire object between screens.

## Room

Room is a persistence library built on SQLite. It provides compile-time validation for
database queries and maps Kotlin classes to database tables and query results.

Room is appropriate for this app because its data is local-first. It can persist:

- Clients and contracts
- Requirement templates and generated requirements
- Due dates and completion status
- Notes and pinned-note state

The main Room concepts are:

- **Entity:** a Kotlin class representing a database table
- **DAO:** an interface defining database reads and writes
- **Database:** the Room database configuration and DAO provider
- **Migration:** a controlled schema change between app versions

`room-ktx` adds Kotlin coroutine and Flow support. A DAO can expose a `Flow`, allowing
Compose screens to update when stored data changes.

`room-compiler` generates Room's implementation code at build time. It should be configured
with KSP, which requires the KSP Gradle plugin in addition to the library dependency.

## WorkManager

WorkManager runs deferrable background work that should complete even if the app exits or
the device restarts. Android chooses an appropriate execution time while respecting any
configured constraints.

In this app, WorkManager can:

- Periodically find due-soon and overdue requirements
- Create local notification reminders
- Reschedule recurring maintenance work after a restart

WorkManager is not an exact alarm system. It is suitable for reliable periodic checks, but
Android may delay execution to preserve battery life.

The KTX artifact supports Kotlin-friendly workers such as `CoroutineWorker`, allowing
background jobs to call suspend functions.

## Hilt

Hilt is a dependency injection framework built on Dagger. It creates objects and supplies
their dependencies, reducing manual construction and making ownership clearer.

Hilt can provide:

- A single Room database instance
- DAOs and repositories
- WorkManager worker dependencies
- ViewModels and their dependencies

Hilt requires an application class annotated with `@HiltAndroidApp`. Android entry points,
such as the main activity, use `@AndroidEntryPoint`. ViewModels use `@HiltViewModel`.

Like Room, Hilt uses generated code. Its compiler should be configured with KSP. Injecting
dependencies directly into WorkManager workers also requires the separate AndroidX Hilt
WorkManager integration.

## Consistent Version Management

This project manages dependency versions and aliases in
`gradle/libs.versions.toml`, then references those aliases from Gradle build files.

Each new dependency should be added to the version catalog rather than placing a version
string directly in `app/build.gradle.kts`. Dependencies that share a release version can
reference the same entry in the catalog.

Compose is the exception in one useful way: its BOM selects compatible versions for the
Compose libraries, so individual Compose artifacts do not need their own versions.

Gradle plugins for Hilt and KSP should also be declared in the version catalog. The
compiler artifacts for Room and Hilt belong in the `ksp(...)` configuration rather than
`implementation(...)`.
