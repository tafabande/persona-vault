@echo off
setlocal enabledelayedexpansion

set "APP_HOME=%~dp0"
set "PROPS_FILE=%APP_HOME%gradle\wrapper\gradle-wrapper.properties"

if not exist "%PROPS_FILE%" (
  echo Missing Gradle wrapper properties: %PROPS_FILE%
  exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0gradlew.ps1" %*
exit /b %errorlevel%
