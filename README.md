# 📅 Client Due Date Tracker

A local-first Android app for tracking contract-driven paperwork deadlines for counseling programs.

---

## 🎯 Purpose

This app is designed to help track required documentation and due dates for clients in structured programs.

It focuses on:

- Tracking paperwork requirements per client
- Calculating due dates automatically
- Surfacing upcoming and overdue work
- Providing quick operational context through notes

---

## 🧠 Core Concepts

- **Client** -> a person in the program
- **Contract** -> defines the program type and rules (one per client)
- **Requirement** -> a piece of required paperwork with a due date
- **Requirement Template** -> defines requirements per contract type
- **Notes** -> lightweight operational context (pinnable)

---

## 📊 Key Features

- 📅 Automatic deadline calculation
- ⚠️ Dashboard for due soon items, overdue items, and pinned notes
- 📝 Notes system with pinning
- 🔔 Local notifications (due soon and overdue)
- 📱 Fully local (no external API)

---

## 🏗️ Tech Stack

- **Kotlin**
- **Jetpack Compose**
- **Material 3 (Compose)**
- **AndroidX Activity Compose**
- **AndroidX Lifecycle Runtime KTX**
- **Navigation Compose**
- **Room**
- **WorkManager**
- **Hilt**
- **Spotless / ktlint**
- **Android Lint**

---

## 🧱 Architecture

Domain-oriented Android architecture:

```text
domain/     Business-focused areas and their UI, state, and models
data/       Persistence and repository implementations
platform/   Android platform integrations
shared/     Reusable UI, navigation, utilities, and theme
```

See [`docs/architecture.md`](docs/architecture.md) for details.

---

## 📚 Developer Docs

- [`docs/architecture.md`](docs/architecture.md) - package ownership and high-level structure
- [`docs/dependencies/DependencyGuide.md`](docs/dependencies/DependencyGuide.md) - core dependency roles
- [`docs/developer-reference.md`](docs/developer-reference.md) - curated official docs links

---

## 🔁 Git and PR Workflow

- Start from `main`.
- Create a short-lived branch for one issue or story.
- Run local validation before opening a PR.
- Open a PR into `main` and link the issue or epic.
- Use squash merge after review and passing CI.
- Delete the branch after merge.

Use the PR template in `.github/pull_request_template.md` for summary, screenshots, test notes,
and linked issue context.

---

## ▶️ Run on Emulator

From the project root:

```powershell
.\tools\start-app.ps1
```

Optional parameters:

```powershell
# Pick a specific AVD name
.\tools\start-app.ps1 -AvdName "Pixel_8_API_36"

# Skip build (faster if APK is already built)
.\tools\start-app.ps1 -SkipBuild

# Launch a standalone emulator window when none are running
.\tools\start-app.ps1 -External
.\tools\start-app.ps1 -e
```

Safety behavior:
- If zero emulators are running, it expects you to start the IDE-integrated emulator first.
- If exactly one emulator is running, it reuses it.
- If more than one emulator is running, it stops with an error so you do not accidentally proceed with multiple emulators.
- Use `-External` (or `-e` / `--e`) if you want the script to launch an external emulator window.
- If `-AvdName` is not provided, it defaults to `Pixel 4 XL` (or falls back to the first available AVD if that profile does not exist).
- Note: launching an IDE-embedded emulator directly is controlled by the IDE, so the script reuses it once it is running.

SDK discovery for `adb`/`emulator`:
- `PATH`
- `ANDROID_SDK_ROOT`
- `ANDROID_HOME`
- `local.properties` (`sdk.dir`)
- `%LOCALAPPDATA%\Android\Sdk`

---

## 🛠️ Project Tools

Run these scripts from the project root:

```powershell
.\tools\build.ps1
.\tools\clean-build.ps1
.\tools\format.ps1
.\tools\lint.ps1
.\tools\test.ps1
.\tools\start-app.ps1
```

Command purpose:

- `.\tools\build.ps1` runs the full Gradle build, including lint, formatting checks, and tests.
- `.\tools\clean-build.ps1` cleans and rebuilds from scratch.
- `.\tools\format.ps1` applies Kotlin formatting.
- `.\tools\lint.ps1` runs Android lint static analysis and writes reports under `app/build/reports/`.
- `.\tools\test.ps1` runs local JVM tests.
- `.\tools\start-app.ps1` builds and installs the debug app on an emulator or connected device.

Test naming:

- Local JVM tests live under `app/src/test`.
- Android-dependent tests live under `app/src/androidTest`.
- Name test classes after the subject under test, such as `RequirementStatusTest` or
  `PersistenceTestTableDaoTest`.

Configure the repo's local Git hooks once per checkout:

```powershell
.\tools\setup-git-hooks.ps1
```

The commit-message hook appends a pull request link when you commit from the CLI. If the GitHub
CLI can find an existing PR for the current branch, it appends that PR URL. Otherwise, it appends
a GitHub compare URL that opens a new PR.
