@echo off
setlocal

set "ROOT=%~dp0"

if "%~1"=="" goto :help
if /I "%~1"=="help" goto :help
if /I "%~1"=="list" goto :help

if /I "%~1"=="start-app" (
  powershell -NoProfile -ExecutionPolicy Bypass -File "%ROOT%scripts\start-app.ps1" %2 %3 %4 %5 %6 %7 %8 %9
  exit /b %errorlevel%
)

echo Unknown command: %~1
echo.
goto :help

:help
echo Project console commands:
echo   console start-app [options]
echo.
echo Default AVD: Pixel 4 XL
echo Example (override):
echo   console start-app -AvdName "Pixel_8_API_36"
echo External emulator:
echo   console start-app -External
echo   console start-app -e
exit /b 0
