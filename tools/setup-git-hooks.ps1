Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot

Push-Location $repoRoot
try {
    git config core.hooksPath .githooks
    Write-Host "Configured Git hooks path: .githooks"
}
finally {
    Pop-Location
}
