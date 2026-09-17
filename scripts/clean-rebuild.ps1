param (
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "Persona Vault: Clean Rebuild & Cache Reset Pipeline" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

# 1. Stop background Gradle daemons to release file locks
Write-Host "[1/5] Stopping active Gradle daemons..." -ForegroundColor Yellow
try {
    & "$projectRoot\gradlew.ps1" --stop
    Write-Host "Gradle daemons stopped." -ForegroundColor Green
} catch {
    Write-Host "Gradle daemon stopped or not running." -ForegroundColor DarkGray
}

# 2. Remove redirected build tree ($HOME/.gradle-builds/PimsVault)
$externalBuildDir = Join-Path $env:USERPROFILE ".gradle-builds\PimsVault"
Write-Host "[2/5] Purging external build cache: $externalBuildDir" -ForegroundColor Yellow
if (Test-Path $externalBuildDir) {
    cmd /c "rmdir /s /q `"$externalBuildDir`""
    if (Test-Path $externalBuildDir) {
        cmd /c "rmdir /s /q \\?\`"$externalBuildDir`""
    }
    Write-Host "External build cache purged." -ForegroundColor Green
} else {
    Write-Host "External build cache is clean." -ForegroundColor Green
}

# 3. Purge workspace caches (.gradle, .kotlin, .firebase, build, app/build)
Write-Host "[3/5] Purging local workspace caches..." -ForegroundColor Yellow
$dirsToPurge = @(
    (Join-Path $projectRoot ".gradle"),
    (Join-Path $projectRoot ".kotlin"),
    (Join-Path $projectRoot ".firebase"),
    (Join-Path $projectRoot "build"),
    (Join-Path $projectRoot "app\build"),
    (Join-Path $projectRoot "app\.cxx")
)

foreach ($dir in $dirsToPurge) {
    if (Test-Path $dir) {
        cmd /c "rmdir /s /q `"$dir`""
    }
}
Write-Host "Workspace caches wiped." -ForegroundColor Green

# 4. Remove root crash logs and screenshots if present
Write-Host "[4/5] Checking for leftover temporary logs and screenshots..." -ForegroundColor Yellow
Get-ChildItem -Path $projectRoot -Filter "*.log" -File -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
Get-ChildItem -Path $projectRoot -Filter "*.png" -File -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
Write-Host "Temporary artifacts clean." -ForegroundColor Green

# 5. Run Gradle clean or targeted task
if ($GradleArgs -and $GradleArgs.Count -gt 0) {
    Write-Host "[5/5] Executing clean build target: $GradleArgs" -ForegroundColor Yellow
    & "$projectRoot\gradlew.ps1" clean @GradleArgs --no-build-cache
} else {
    Write-Host "[5/5] Verifying Gradle project initialization with clean..." -ForegroundColor Yellow
    & "$projectRoot\gradlew.ps1" clean --no-build-cache
    Write-Host "Clean build initialization completed successfully!" -ForegroundColor Green
    Write-Host "Ready for fresh build: run '.\scripts\clean-rebuild.ps1 assembleDebug' or '.\gradlew.ps1 assembleDebug'" -ForegroundColor Cyan
}

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "Fresh state ready. Old builds and caches eradicated." -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Cyan
