Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$gradleWrapper = Join-Path $repoRoot "gradlew.bat"

Push-Location $repoRoot
try {
    & $gradleWrapper spotlessApply
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
