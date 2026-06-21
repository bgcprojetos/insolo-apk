@echo off
setlocal EnableExtensions
set "GRADLE_VERSION=9.4.1"
set "DIST_ROOT=%USERPROFILE%\.gradle\insolo-gradle\gradle-%GRADLE_VERSION%"
set "GRADLE_HOME=%DIST_ROOT%\gradle-%GRADLE_VERSION%"
set "ZIP_FILE=%DIST_ROOT%\gradle-%GRADLE_VERSION%-bin.zip"
set "URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"

if exist "%GRADLE_HOME%\bin\gradle.bat" goto run

if not exist "%DIST_ROOT%" mkdir "%DIST_ROOT%"
if not exist "%ZIP_FILE%" (
  echo Baixando Gradle %GRADLE_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%URL%' -OutFile '%ZIP_FILE%'"
  if errorlevel 1 exit /b %errorlevel%
)

echo Preparando Gradle %GRADLE_VERSION%...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP_FILE%' -DestinationPath '%DIST_ROOT%' -Force"
if errorlevel 1 exit /b %errorlevel%

:run
call "%GRADLE_HOME%\bin\gradle.bat" %*
endlocal
