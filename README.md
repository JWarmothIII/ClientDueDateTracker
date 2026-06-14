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

---

## 🧱 Architecture

DDD-lite layered architecture:

```text
presentation -> application -> domain -> infrastructure
```

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
.\tools\test.ps1
.\tools\start-app.ps1
```
