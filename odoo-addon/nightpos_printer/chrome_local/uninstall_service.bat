@echo off
REM NightPOS Print Server — Remove Windows Service
REM Run as Administrator
cd /d %~dp0

title NightPOS — Uninstall Service

echo ============================================================
echo  NightPOS Print Server — Remove Windows Service (NSSM)
echo ============================================================
echo.

REM ── Check Administrator ──────────────────────────────────────
net session >nul 2>&1
if errorlevel 1 (
    echo ERROR: This script must be run as Administrator.
    echo Right-click uninstall_service.bat and choose "Run as administrator".
    pause
    exit /b 1
)

set SVC_NAME=NightPOSPrintServer
set NSSM=%~dp0nssm.exe

REM ── Stop and remove via NSSM ─────────────────────────────────
sc query "%SVC_NAME%" >nul 2>&1
if errorlevel 1 (
    echo Service "%SVC_NAME%" is not installed.
    pause
    exit /b 0
)

echo  Stopping service...
if exist "%NSSM%" (
    "%NSSM%" stop "%SVC_NAME%"
    timeout /t 3 /nobreak >nul
    echo  Removing service...
    "%NSSM%" remove "%SVC_NAME%" confirm
) else (
    sc stop "%SVC_NAME%" >nul 2>&1
    timeout /t 3 /nobreak >nul
    sc delete "%SVC_NAME%"
)

if errorlevel 1 (
    echo ERROR: Could not remove service. Try manually: sc delete %SVC_NAME%
) else (
    echo.
    echo  Service "%SVC_NAME%" removed successfully.
)

echo.
pause
