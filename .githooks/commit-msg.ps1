param(
    [Parameter(Mandatory = $true)]
    [string] $MessageFile
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $MessageFile)) {
    exit 0
}

$commitMessage = Get-Content -Raw -LiteralPath $MessageFile
if ($commitMessage -match "(?m)^(Pull request|Open pull request): https://github\.com/") {
    exit 0
}

$branch = (& git branch --show-current 2>$null).Trim()
if ([string]::IsNullOrWhiteSpace($branch)) {
    exit 0
}

if ($branch -in @("main", "master")) {
    exit 0
}

$remoteUrl = (& git config --get remote.origin.url 2>$null).Trim()
$repoPath = $null

if ($remoteUrl -match "^git@github\.com:(?<repo>.+?)(\.git)?$") {
    $repoPath = $Matches.repo -replace "\.git$", ""
}
elseif ($remoteUrl -match "^https?://github\.com/(?<repo>.+?)(\.git)?$") {
    $repoPath = $Matches.repo -replace "\.git$", ""
}

if ([string]::IsNullOrWhiteSpace($repoPath)) {
    exit 0
}

$repoUrl = "https://github.com/$repoPath"
$pullRequestUrl = $null

if (Get-Command gh -ErrorAction SilentlyContinue) {
    $pullRequestUrl = (& gh pr view $branch --repo $repoPath --json url --jq .url 2>$null)
    if ($LASTEXITCODE -ne 0) {
        $pullRequestUrl = $null
    }
}

if (-not [string]::IsNullOrWhiteSpace($pullRequestUrl)) {
    $linkLine = "Pull request: $pullRequestUrl"
}
else {
    $baseBranch = (& git symbolic-ref --short refs/remotes/origin/HEAD 2>$null)
    if ($LASTEXITCODE -eq 0) {
        $baseBranch = $baseBranch -replace "^origin/", ""
    }
    if ([string]::IsNullOrWhiteSpace($baseBranch)) {
        $baseBranch = "main"
    }

    $linkLine = "Open pull request: $repoUrl/compare/$baseBranch...$branch`?expand=1"
}

Add-Content -LiteralPath $MessageFile -Value ""
Add-Content -LiteralPath $MessageFile -Value $linkLine
