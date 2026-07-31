# Client Due Date Tracker

A local-first Android app for tracking contract-driven counseling paperwork deadlines.

## V1 capabilities

- Atomic client onboarding with one contract and generated initial requirements
- Configurable, versioned CTS, USPO, and State / RSUD requirement definitions
- Rolling 30-day deadline generation for intake, assessment, event, exit, weekly, monthly, and
  90-day policies
- Dashboard sections for overdue, due-soon, recently completed, and pinned-note context
- Quick requirement completion and reopening
- Client-owned notes with optional requirement links and multiple pins
- One generic daily notification summary with no client-specific lock-screen content
- Fully local application data with Android backup and device transfer enabled

The dashboard is the start destination. Use **Add client** to enter initials, intake and assessment
dates, an optional completed-assessment or planned-exit date, and an active contract type. Dates use
`YYYY-MM-DD`.

## Technology

- Kotlin and Jetpack Compose with Material 3
- Room with an exported canonical version-1 schema
- Hilt for dependency injection
- WorkManager for local daily maintenance
- Navigation Compose
- Spotless/ktlint and Android lint

## Architecture

The single `app` Gradle module is organized around four write-owning domains:
`client`, `contractdefinition`, `contracttracking`, and `notes`. `dashboard` and `notification`
consume their public APIs.

Business models are framework-free, IDs are typed UUID strings, and Room numeric keys never cross
owner APIs. There are no generic `shared` or `core` packages.

- [Architecture](docs/architecture.md)
- [V1 ERD](docs/erd.md)
- [Screen conventions](docs/screen-conventions.md)
- [Decision journal](docs/decision-journal.md)
- [Dependency guide](docs/dependencies/DependencyGuide.md)
- [Developer reference](docs/developer-reference.md)

## Run on an emulator

```powershell
.\tools\start-app.ps1
```

Use `-AvdName` to choose a profile, `-SkipBuild` to reuse an APK, or `-External`/`-e` to launch a
standalone emulator. The script refuses to guess when multiple emulators are connected.

Existing development installs using the old persistence-proof schema must be cleared or
reinstalled before running this version.

## Local verification

```powershell
.\tools\format.ps1
.\tools\test.ps1
.\tools\lint.ps1
.\tools\build.ps1
```

- JVM business and architecture tests live under `app/src/test`.
- Room and Compose instrumentation tests live under `app/src/androidTest`.
- The full build checks formatting, unit tests, lint, APK assembly, and instrumentation-test
  compilation. Running instrumentation tests still requires an emulator or device.

## Git workflow

Create a short-lived branch from `main`, keep one issue or story in scope, run local validation,
and open a pull request using `.github/pull_request_template.md`. Do not claim remote CI passed
until the branch-specific workflow has actually completed.
