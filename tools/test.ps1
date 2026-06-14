Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$gradleWrapper = Join-Path $repoRoot "gradlew.bat"

Push-Location $repoRoot
try {
    & $gradleWrapper test
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
