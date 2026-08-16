@echo off
setlocal
set ROOT=%~dp0
set GD=%ROOT%.gradle-local\gradle-8.9
set ZIP=%ROOT%.gradle-local\gradle-8.9-bin.zip
if not exist "%GD%\bin\gradle.bat" (
  if not exist "%ROOT%.gradle-local" mkdir "%ROOT%.gradle-local"
  echo [SayIt] Downloading Gradle 8.9 once...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-8.9-bin.zip' -OutFile '%ZIP%'"
  if errorlevel 1 exit /b 1
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ZIP%' '%ROOT%.gradle-local'"
  if errorlevel 1 exit /b 1
)
call "%GD%\bin\gradle.bat" %*
endlocal
