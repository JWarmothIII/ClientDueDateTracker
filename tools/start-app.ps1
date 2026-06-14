param(
    [string]$AvdName,
    [switch]$SkipBuild,
    [Alias("e")]
    [switch]$External
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-LocalPropertiesValue {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string]$Key
    )

    if (-not (Test-Path $FilePath)) {
        return $null
    }

    $pattern = "^\s*{0}\s*=(.*)$" -f [regex]::Escape($Key)
    foreach ($line in Get-Content $FilePath) {
        if ($line -match $pattern) {
            return $matches[1].Trim()
        }
    }

    return $null
}

function Unescape-GradlePath {
    param([string]$Value)

    if (-not $Value) {
        return $null
    }

    return $Value.Replace("\:", ":").Replace("\\", "\")
}

function Get-AndroidSdkRoots {
    param([Parameter(Mandatory = $true)][string]$RepoRoot)

    $candidateRoots = New-Object System.Collections.Generic.List[string]

    if ($env:ANDROID_SDK_ROOT) {
        [void]$candidateRoots.Add($env:ANDROID_SDK_ROOT)
    }
    if ($env:ANDROID_HOME) {
        [void]$candidateRoots.Add($env:ANDROID_HOME)
    }

    $localPropertiesPath = Join-Path $RepoRoot "local.properties"
    $sdkDir = Get-LocalPropertiesValue -FilePath $localPropertiesPath -Key "sdk.dir"
    if ($sdkDir) {
        [void]$candidateRoots.Add((Unescape-GradlePath -Value $sdkDir))
    }

    if ($env:LOCALAPPDATA) {
        [void]$candidateRoots.Add((Join-Path $env:LOCALAPPDATA "Android\Sdk"))
    }
    if ($env:USERPROFILE) {
        [void]$candidateRoots.Add((Join-Path $env:USERPROFILE "AppData\Local\Android\Sdk"))
    }

    $seen = @{}
    $roots = @()
    foreach ($candidate in $candidateRoots) {
        if ([string]::IsNullOrWhiteSpace($candidate)) {
            continue
        }
        try {
            $fullPath = [System.IO.Path]::GetFullPath($candidate)
        } catch {
            continue
        }
        if ($seen.ContainsKey($fullPath)) {
            continue
        }
        $seen[$fullPath] = $true
        if (Test-Path $fullPath) {
            $roots += $fullPath
        }
    }

    return $roots
}

function Get-AndroidToolPath {
    param(
        [Parameter(Mandatory = $true)][string]$ToolName,
        [Parameter(Mandatory = $true)][string]$RelativePath,
        [Parameter(Mandatory = $true)][string[]]$SdkRoots
    )

    foreach ($root in $SdkRoots) {
        $candidate = Join-Path $root $RelativePath
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    $cmd = Get-Command $ToolName -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }

    $rootsMessage = if ($SdkRoots.Count -gt 0) { ($SdkRoots -join "; ") } else { "(none found)" }
    throw "Could not find '$ToolName'. Checked PATH and SDK roots: $rootsMessage. Set ANDROID_SDK_ROOT/ANDROID_HOME or local.properties sdk.dir."
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$gradlew = Join-Path $repoRoot "gradlew.bat"
if (-not (Test-Path $gradlew)) {
    throw "gradlew.bat not found at '$gradlew'. Run this script from inside the project."
}

$sdkRoots = Get-AndroidSdkRoots -RepoRoot $repoRoot
$adb = Get-AndroidToolPath -ToolName "adb" -RelativePath "platform-tools\adb.exe" -SdkRoots $sdkRoots
$emulator = Get-AndroidToolPath -ToolName "emulator" -RelativePath "emulator\emulator.exe" -SdkRoots $sdkRoots

if (-not $AvdName) {
    $avds = @(
        & $emulator -list-avds |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -match "^[A-Za-z0-9][A-Za-z0-9._-]*$" }
    )
    if (-not $avds -or $avds.Count -eq 0) {
        throw "No Android Virtual Devices found. Create one in Android Studio Device Manager."
    }

    $preferredAvd = $avds |
        Where-Object {
            $normalized = ($_ -replace "[\s_-]", "").ToLowerInvariant()
            $normalized -eq "pixel4xl" -or $normalized.StartsWith("pixel4xlapi")
        } |
        Select-Object -First 1

    if ($preferredAvd) {
        $AvdName = $preferredAvd
    } else {
        $AvdName = $avds[0]
        Write-Host "Default AVD 'Pixel 4 XL' not found. Using '$AvdName'."
    }
}

$existingSerials = @(
    & $adb devices |
    Select-String -Pattern "^emulator-\d+\s+(device|offline)$" |
    ForEach-Object { $_.ToString().Split([char[]]" `t", [System.StringSplitOptions]::RemoveEmptyEntries)[0] }
)

$targetSerial = $null
if (-not $existingSerials -or $existingSerials.Count -eq 0) {
    if (-not $External) {
        throw "No running emulator found. Start your IDE integrated emulator first (recommended AVD: '$AvdName'), or run with -External to launch a standalone emulator window."
    }

    Write-Host "Starting external emulator '$AvdName'..."
    Start-Process -FilePath $emulator -ArgumentList "-avd", $AvdName | Out-Null

    Write-Host "Waiting for emulator to connect..."
    for ($i = 0; $i -lt 60; $i++) {
        $currentSerials = @(
            & $adb devices |
            Select-String -Pattern "^emulator-\d+\s+(device|offline)$" |
            ForEach-Object { $_.ToString().Split([char[]]" `t", [System.StringSplitOptions]::RemoveEmptyEntries)[0] }
        )

        $newSerial = $currentSerials | Where-Object { $_ -notin $existingSerials } | Select-Object -First 1
        if ($newSerial) {
            $targetSerial = $newSerial
            break
        }
        Start-Sleep -Seconds 2
    }

    if (-not $targetSerial) {
        throw "Started emulator but could not detect its serial in adb devices."
    }

    & $adb -s $targetSerial wait-for-device
} elseif ($existingSerials.Count -eq 1) {
    Write-Host "An emulator is already running. Reusing existing emulator."
    $targetSerial = $existingSerials[0]
} else {
    $serialList = $existingSerials -join ", "
    throw "More than one emulator is running ($serialList). Close extras, then re-run this script to avoid accidental multi-emulator launches."
}

Write-Host "Waiting for Android boot completion..."
for ($i = 0; $i -lt 120; $i++) {
    $boot = (& $adb -s $targetSerial shell getprop sys.boot_completed 2>$null).Trim()
    if ($boot -eq "1") {
        break
    }
    Start-Sleep -Seconds 2
}

$boot = (& $adb -s $targetSerial shell getprop sys.boot_completed 2>$null).Trim()
if ($boot -ne "1") {
    throw "Emulator did not finish booting within timeout."
}

if (-not $SkipBuild) {
    Write-Host "Building debug APK..."
    & $gradlew :app:assembleDebug
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle assembleDebug failed."
    }
}

if ($SkipBuild) {
    $apkPath = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path $apkPath)) {
        throw "SkipBuild was set, but APK was not found at '$apkPath'. Build first or run without -SkipBuild."
    }
    Write-Host "Installing existing APK..."
    & $adb -s $targetSerial install -r $apkPath | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "adb install failed."
    }
} else {
    Write-Host "Installing app..."
    & $gradlew :app:installDebug
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle installDebug failed."
    }
}

Write-Host "Launching app..."
& $adb -s $targetSerial shell am start -n "dev.jwarmothiii.clientduedatetracker/.MainActivity" | Out-Null

Write-Host ""
Write-Host "Done. App launched on $targetSerial."
