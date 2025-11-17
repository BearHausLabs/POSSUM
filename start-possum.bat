@echo off
REM ========================================
REM  POSSUM - Start Server
REM ========================================

echo.
echo ========================================
echo   Starting POSSUM Server
echo ========================================
echo.

REM Change to the correct directory
cd /d "%~dp0"

REM Check if already running
tasklist /FI "WINDOWTITLE eq POSSUM*" 2>NUL | find /I /N "java.exe">NUL
if "%ERRORLEVEL%"=="0" (
    echo [WARNING] POSSUM appears to be already running!
    echo Run stop-possum.bat first if you want to restart.
    echo.
    pause
    exit /b 1
)

REM Start the server
echo Starting POSSUM in simulator mode...
echo Press Ctrl+C to stop the server
echo.

set SIMULATION_MODE=true
gradlew.bat bootRun -Dposkeyboard.enabled=true -Dtoneindicator.enabled=true -Dkeylock.enabled=true

echo.
echo Server stopped.
pause

