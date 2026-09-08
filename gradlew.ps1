$ErrorActionPreference = 'Stop'

if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME")) {
    $userJava = [Environment]::GetEnvironmentVariable("JAVA_HOME", "User")
    if ($userJava -and (Test-Path $userJava)) {
        $env:JAVA_HOME = $userJava
        $env:Path = "$userJava\bin;$env:Path"
    }
}

$appHome = Split-Path -Parent $MyInvocation.MyCommand.Path
$propsFile = Join-Path $appHome 'gradle\wrapper\gradle-wrapper.properties'

if (-not (Test-Path $propsFile)) {
    throw "Missing Gradle wrapper properties: $propsFile"
}

$distUrlLine = Get-Content $propsFile | Where-Object { $_ -like 'distributionUrl=*' } | Select-Object -First 1
if (-not $distUrlLine) {
    throw "Unable to read distributionUrl from $propsFile"
}

$distUrl = $distUrlLine.Substring($distUrlLine.IndexOf('=') + 1).Replace('\:', ':')
$versionMatch = [regex]::Match($distUrl, 'gradle-(.+?)-bin\.zip')
if (-not $versionMatch.Success) {
    throw "Unable to parse Gradle version from $distUrl"
}

$gradleVersion = $versionMatch.Groups[1].Value
$gradleUserHome = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE '.gradle' }
$installDir = Join-Path $gradleUserHome "wrapper\dists\gradle-$gradleVersion-bin"
$gradleExe = Join-Path $installDir "gradle-$gradleVersion\bin\gradle.bat"

if (-not (Test-Path $gradleExe)) {
    New-Item -ItemType Directory -Force -Path $installDir | Out-Null
    $tmpDir = Join-Path $installDir ("tmp-" + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null
    $zipPath = Join-Path $tmpDir "gradle-$gradleVersion-bin.zip"

    Invoke-WebRequest -Uri $distUrl -OutFile $zipPath
    Expand-Archive -Path $zipPath -DestinationPath $tmpDir -Force

    $extractedDir = Join-Path $tmpDir "gradle-$gradleVersion"
    if (-not (Test-Path $extractedDir)) {
        throw "Downloaded Gradle archive did not contain gradle-$gradleVersion."
    }

    Remove-Item -Recurse -Force (Join-Path $installDir "gradle-$gradleVersion") -ErrorAction SilentlyContinue
    Move-Item -Force $extractedDir $installDir
    Remove-Item -Recurse -Force $tmpDir
}

& $gradleExe @args
