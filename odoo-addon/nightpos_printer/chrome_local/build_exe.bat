@echo off
REM NightPOS Print Server — build standalone EXE with PyInstaller
cd /d %~dp0

title NightPOS — Build EXE

echo ============================================================
echo  NightPOS Print Server — Build Standalone EXE
echo ============================================================
echo.

REM Create/reuse venv
if not exist ".venv\Scripts\python.exe" (
    echo [1/4] Creating virtual environment...
    python -m venv .venv
    if errorlevel 1 ( echo ERROR: Python not found. & pause & exit /b 1 )
) else (
    echo [1/4] Virtual environment exists, skipping.
)

echo [2/4] Installing dependencies + PyInstaller...
.venv\Scripts\pip install --upgrade pip --quiet
.venv\Scripts\pip install websockets pywin32 pyinstaller --quiet
if errorlevel 1 ( echo ERROR: pip install failed. & pause & exit /b 1 )

echo [3/4] Building NightPOS_PrintServer.exe...
.venv\Scripts\pyinstaller ^
    --onefile ^
    --console ^
    --name "NightPOS_PrintServer" ^
    --hidden-import=win32timezone ^
    --hidden-import=win32print ^
    --hidden-import=win32ui ^
    local_print_server.py
if errorlevel 1 ( echo ERROR: PyInstaller failed. & pause & exit /b 1 )

echo [4/4] Copying output...
if exist "dist\NightPOS_PrintServer.exe" (
    copy /Y "dist\NightPOS_PrintServer.exe" "NightPOS_PrintServer.exe" >nul
    echo.
    echo  Ready: NightPOS_PrintServer.exe
    echo  Run install_service.bat (as Administrator) to install as a Windows service.
) else (
    echo ERROR: Output EXE not found in dist\.
    pause
    exit /b 1
)

echo.
pause
