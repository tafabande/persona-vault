$ErrorActionPreference = 'Stop'
$sdkDir = "C:\Users\DEV\AppData\Local\Android\Sdk"
$platformsDir = Join-Path $sdkDir "platforms"
$buildToolsDir = Join-Path $sdkDir "build-tools"
$tmpDir = Join-Path $sdkDir "tmp_dl"

New-Item -ItemType Directory -Force -Path $platformsDir | Out-Null
New-Item -ItemType Directory -Force -Path $buildToolsDir | Out-Null
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

$platformZip = Join-Path $tmpDir "platform-34.zip"
$buildToolsZip = Join-Path $tmpDir "build-tools-34.zip"

Write-Host "Downloading Platform 34..."
curl.exe -L -o $platformZip "https://dl.google.com/android/repository/platform-34-ext7_r03.zip"

Write-Host "Downloading Build-Tools 34..."
curl.exe -L -o $buildToolsZip "https://dl.google.com/android/repository/build-tools_r34-windows.zip"

Write-Host "Extracting Platform 34..."
$platTmp = Join-Path $tmpDir "plat"
New-Item -ItemType Directory -Force -Path $platTmp | Out-Null
tar.exe -xf $platformZip -C $platTmp
$platExtracted = Get-ChildItem -Path $platTmp -Directory | Select-Object -First 1
$targetPlat = Join-Path $platformsDir "android-34"
if (Test-Path $targetPlat) { Remove-Item -Recurse -Force $targetPlat }
Move-Item -Path $platExtracted.FullName -Destination $targetPlat

Write-Host "Extracting Build-Tools 34..."
$btTmp = Join-Path $tmpDir "bt"
New-Item -ItemType Directory -Force -Path $btTmp | Out-Null
tar.exe -xf $buildToolsZip -C $btTmp
$btExtracted = Get-ChildItem -Path $btTmp -Directory | Select-Object -First 1
$targetBt = Join-Path $buildToolsDir "34.0.0"
if (Test-Path $targetBt) { Remove-Item -Recurse -Force $targetBt }
Move-Item -Path $btExtracted.FullName -Destination $targetBt

Remove-Item -Recurse -Force $tmpDir -ErrorAction SilentlyContinue
Write-Host "SDK Packages Platform 34 and Build-Tools 34.0.0 installed successfully!"
