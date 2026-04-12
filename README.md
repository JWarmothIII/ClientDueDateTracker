# ClientDueDateTracker

## Run on Emulator

From the project root:

```powershell
.\console start-app
```

Optional parameters:

```powershell
# Pick a specific AVD name
.\console start-app -AvdName "Pixel_8_API_36"

# Skip build (faster if APK is already built)
.\console start-app -SkipBuild

# Launch a standalone emulator window when none are running
.\console start-app -External
```

Safety behavior:
- If zero emulators are running, it expects you to start the IDE-integrated emulator first.
- If exactly one emulator is running, it reuses it.
- If more than one emulator is running, it stops with an error so you do not accidentally proceed with multiple emulators.
- Use `-External` if you want the script to launch an external emulator window.
- If `-AvdName` is not provided, it defaults to `Pixel 4 XL` (or falls back to the first available AVD if that profile does not exist).
- Note: launching an IDE-embedded emulator directly is controlled by the IDE, so the script reuses it once it is running.

SDK discovery for `adb`/`emulator`:
- `PATH`
- `ANDROID_SDK_ROOT`
- `ANDROID_HOME`
- `local.properties` (`sdk.dir`)
- `%LOCALAPPDATA%\Android\Sdk`

## Project Console

This repo includes a lightweight command dispatcher:

- `console start-app` (Command Prompt)
- `.\console start-app` (PowerShell)
- `.\console.ps1 start-app` (PowerShell explicit)
