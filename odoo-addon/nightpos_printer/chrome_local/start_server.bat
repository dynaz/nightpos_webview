@echo off
REM NightPOS Local Print Server — launcher
REM Activates the venv and starts the WebSocket print server.

title NightPOS Print Server

if not exist ".venv\Scripts\python.exe" (
    echo Virtual environment not found. Run install.bat first.
    pause
    exit /b 1
)

echo Starting NightPOS Print Server on ws://localhost:8765 ...
echo Press Ctrl+C to stop.
echo.

.venv\Scripts\python local_print_server.py
pause
