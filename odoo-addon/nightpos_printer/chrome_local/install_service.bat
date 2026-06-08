@echo off
REM NightPOS Print Server — Install as Windows Service using NSSM
REM Run as Administrator
cd /d %~dp0

title NightPOS — Install Service

echo ============================================================
echo  NightPOS Print Server — Install Windows Service (NSSM)
echo ============================================================
echo.

REM ── Check Administrator ──────────────────────────────────────
net session >nul 2>&1
if errorlevel 1 (
    echo ERROR: This script must be run as Administrator.
    echo Right-click install_service.bat and choose "Run as administrator".
    pause
    exit /b 1
)

REM ── Locate server EXE ────────────────────────────────────────
set SERVER_EXE=%~dp0NightPOS_PrintServer.exe
if not exist "%SERVER_EXE%" (
    if exist "%~dp0dist\NightPOS_PrintServer.exe" (
        set SERVER_EXE=%~dp0dist\NightPOS_PrintServer.exe
    ) else (
        echo ERROR: NightPOS_PrintServer.exe not found.
        echo Run build_exe.bat first.
        pause
        exit /b 1
    )
)
echo  Server EXE : %SERVER_EXE%

REM ── Download NSSM if not present ─────────────────────────────
set NSSM=%~dp0nssm.exe
if not exist "%NSSM%" (
    echo  NSSM not found — downloading nssm 2.24...
    powershell -NoProfile -Command ^
        "Invoke-WebRequest -Uri 'https://nssm.cc/release/nssm-2.24.zip' -OutFile '%TEMP%\nssm.zip'"
    if errorlevel 1 (
        echo ERROR: Failed to download NSSM. Check internet connection.
        pause
        exit /b 1
    )
    powershell -NoProfile -Command ^
        "Expand-Archive -Path '%TEMP%\nssm.zip' -DestinationPath '%TEMP%\nssm_extract' -Force"
    copy /Y "%TEMP%\nssm_extract\nssm-2.24\win64\nssm.exe" "%NSSM%" >nul
    if errorlevel 1 (
        echo ERROR: Could not extract nssm.exe.
        pause
        exit /b 1
    )
    echo  NSSM downloaded: %NSSM%
) else (
    echo  NSSM found   : %NSSM%
)

REM ── Service settings ─────────────────────────────────────────
set SVC_NAME=NightPOSPrintServer
set SVC_DISPLAY=NightPOS Print Server
set SVC_DESC=NightPOS local ESC/POS print server for the Chrome extension (ws://localhost:8765)
set LOG_DIR=%~dp0logs
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

REM ── Remove old service if exists ─────────────────────────────
sc query "%SVC_NAME%" >nul 2>&1
if not errorlevel 1 (
    echo  Removing existing service...
    "%NSSM%" stop "%SVC_NAME%" >nul 2>&1
    "%NSSM%" remove "%SVC_NAME%" confirm >nul 2>&1
)

REM ── Install service ──────────────────────────────────────────
echo.
echo  Installing service "%SVC_NAME%"...
"%NSSM%" install "%SVC_NAME%" "%SERVER_EXE%"
if errorlevel 1 ( echo ERROR: NSSM install failed. & pause & exit /b 1 )

"%NSSM%" set "%SVC_NAME%" DisplayName   "%SVC_DISPLAY%"
"%NSSM%" set "%SVC_NAME%" Description   "%SVC_DESC%"
"%NSSM%" set "%SVC_NAME%" Start         SERVICE_AUTO_START
"%NSSM%" set "%SVC_NAME%" AppStdout     "%LOG_DIR%\server.log"
"%NSSM%" set "%SVC_NAME%" AppStderr     "%LOG_DIR%\server.log"
"%NSSM%" set "%SVC_NAME%" AppRotateFiles 1
"%NSSM%" set "%SVC_NAME%" AppRotateBytes 5242880
"%NSSM%" set "%SVC_NAME%" AppRestartDelay 3000

REM ── Start service ────────────────────────────────────────────
echo  Starting service...
"%NSSM%" start "%SVC_NAME%"
if errorlevel 1 (
    echo WARNING: Service installed but failed to start. Check logs at:
    echo   %LOG_DIR%\server.log
) else (
    echo.
    echo  Service installed and started successfully!
    echo  Name    : %SVC_NAME%
    echo  Starts  : Automatically at login
    echo  Logs    : %LOG_DIR%\server.log
    echo  URL     : ws://localhost:8765
)

echo.
echo  To manage the service:
echo    sc start %SVC_NAME%
echo    sc stop  %SVC_NAME%
echo    sc query %SVC_NAME%
echo.
pause
