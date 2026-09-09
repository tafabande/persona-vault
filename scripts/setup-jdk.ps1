$ErrorActionPreference = 'Stop'
$jdkDir = "C:\Users\bleig\.jdks"
if (-not (Test-Path $jdkDir)) {
    New-Item -ItemType Directory -Force -Path $jdkDir | Out-Null
}
$zipPath = Join-Path $jdkDir "temurin17.zip"

Write-Host "Downloading Eclipse Temurin JDK 17..."
curl.exe -L -o $zipPath "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse"

Write-Host "Extracting JDK..."
tar.exe -xf $zipPath -C $jdkDir
if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}

$extractedDir = Get-ChildItem -Path $jdkDir -Directory | Where-Object { $_.Name -like 'jdk-17*' } | Select-Object -First 1
$targetDir = Join-Path $jdkDir "temurin-17"

if ($extractedDir -and ($extractedDir.FullName -ne $targetDir)) {
    if (Test-Path $targetDir) {
        Remove-Item -Recurse -Force $targetDir
    }
    Rename-Item -Path $extractedDir.FullName -NewName "temurin-17"
}

Write-Host "Configuring User Environment Variables..."
[Environment]::SetEnvironmentVariable("JAVA_HOME", $targetDir, "User")

$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
$binDir = Join-Path $targetDir "bin"
if ($userPath -notlike "*$binDir*") {
    $newUserPath = "$binDir;$userPath"
    [Environment]::SetEnvironmentVariable("Path", $newUserPath, "User")
}

$env:JAVA_HOME = $targetDir
$env:Path = "$binDir;$env:Path"

Write-Host "Verifying Java installation:"
& "$binDir\java.exe" -version
& "$binDir\javac.exe" -version
Write-Host "JDK 17 installation complete!"
