# console.ps1
param(
    [string]$Command,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$RemainingItems,
    [Alias("external")]
    [switch]$e
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Show-Help {
    Write-Host "Project console commands:"
    Write-Host "  .\console start-app [options]"
    Write-Host ""
    Write-Host "Default AVD: Pixel 4 XL"
    Write-Host "Example (override):"
    Write-Host "  .\console start-app -AvdName `"Pixel_8_API_36`""
    Write-Host "External emulator:"
    Write-Host "  .\console start-app -External"
    Write-Host "  .\console start-app -e"
    Write-Host "  .\console start-app --e"
}

if ([string]::IsNullOrWhiteSpace($Command)) {
    Show-Help
    exit 0
}

if ($null -eq $RemainingItems) {
    $RemainingItems = @()
}

$repoRoot = Split-Path -Parent $PSCommandPath

switch ($Command.ToLowerInvariant()) {
    "help" {
        Show-Help
        exit 0
    }
    "list" {
        Show-Help
        exit 0
    }
    "start-app" {
        $scriptPath = Join-Path $repoRoot "scripts\start-app.ps1"
        $scriptParams = @{}
        if ($e) {
            $scriptParams["External"] = $true
        }
        for ($i = 0; $i -lt $RemainingItems.Length; $i++) {
            $token = $RemainingItems[$i]
            switch ($token.ToLowerInvariant()) {
                "-avdname" {
                    if ($i + 1 -ge $RemainingItems.Length) {
                        throw "Missing value for -AvdName"
                    }
                    $scriptParams["AvdName"] = $RemainingItems[$i + 1]
                    $i++
                }
                "-skipbuild" {
                    $scriptParams["SkipBuild"] = $true
                }
                "-external" {
                    $scriptParams["External"] = $true
                }
                "-e" {
                    $scriptParams["External"] = $true
                }
                "--external" {
                    $scriptParams["External"] = $true
                }
                "--e" {
                    $scriptParams["External"] = $true
                }
                default {
                    throw "Unknown option for start-app: $token"
                }
            }
        }

        & $scriptPath @scriptParams
        exit $LASTEXITCODE
    }
    default {
        Write-Error "Unknown command: $Command"
        exit 1
    }
}
