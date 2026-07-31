# Developer Reference

Use this page as a starting point when you need official documentation for the
project's core Android stack.

## Compose

- Jetpack Compose overview: https://developer.android.com/compose
- Material 3 in Compose: https://developer.android.com/develop/ui/compose/designsystems/material3
- Compose previews: https://developer.android.com/develop/ui/compose/tooling/previews

## Navigation

- Navigation Compose: https://developer.android.com/develop/ui/compose/navigation
- Navigation testing: https://developer.android.com/guide/navigation/testing

## ViewModel and State

- ViewModel overview: https://developer.android.com/topic/libraries/architecture/viewmodel
- UI layer: https://developer.android.com/topic/architecture/ui-layer
- State holders and UI state: https://developer.android.com/topic/architecture/ui-layer/stateholders
- UI events: https://developer.android.com/topic/architecture/ui-layer/events
- Android architecture recommendations: https://developer.android.com/topic/architecture/recommendations
- State and Jetpack Compose: https://developer.android.com/develop/ui/compose/state
- Lifecycle-aware state collection: https://developer.android.com/develop/ui/compose/state#other-supported-types

## Reference Apps

- Now in Android architecture:
  https://github.com/android/nowinandroid/blob/main/docs/ArchitectureLearningJourney.md
- Android architecture samples: https://github.com/android/architecture-samples
- Official Jetpack Compose samples: https://github.com/android/compose-samples

## Persistence

- Room overview: https://developer.android.com/training/data-storage/room
- Room relationships: https://developer.android.com/training/data-storage/room/relationships
- Room testing: https://developer.android.com/training/data-storage/room/testing-db
- Project V1 ERD: `docs/erd.md`

## Background Work

- WorkManager overview: https://developer.android.com/topic/libraries/architecture/workmanager
- WorkManager testing: https://developer.android.com/topic/libraries/architecture/workmanager/how-to/integration-testing
- Notification runtime permission: https://developer.android.com/develop/ui/views/notifications/notification-permission
- Notification privacy and lock-screen visibility:
  https://developer.android.com/develop/ui/views/notifications/build-notification#lockscreenNotification

## Dependency Injection

- Hilt overview: https://developer.android.com/training/dependency-injection/hilt-android
- Hilt and Jetpack integrations: https://developer.android.com/training/dependency-injection/hilt-jetpack

## Testing and Quality

- Android testing overview: https://developer.android.com/training/testing
- Android lint: https://developer.android.com/studio/write/lint
- Gradle build scans and troubleshooting: https://docs.gradle.org/current/userguide/troubleshooting.html

## Project Docs

- Architecture summary: `docs/architecture.md`
- Screen conventions and template: `docs/screen-conventions.md`
- Dependency guide: `docs/dependencies/DependencyGuide.md`
- Daily commands: `README.md`
- Durable product and architecture decisions: `docs/decision-journal.md`

## Project-specific implementation notes

- `ClientDueDateDatabase` version 1 is the canonical baseline; old development installs are not
  migrated.
- The worker is a unique self-scheduling one-time chain targeting 10:00 AM local time.
- Android 13+ notification permission is requested only after the in-app explanation.
- Business dates use `LocalDate`; audit and delivery moments use UTC `Instant`.
- Generated Room schemas are committed from `app/schemas`.
