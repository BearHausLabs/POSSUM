@echo off
REM ========================================
REM  POSSUM - Start Server (Hardware Mode)
REM ========================================

echo.
echo ========================================
echo   Starting POSSUM Server
echo   MODE: Hardware (Real Devices)
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

REM Start the server in HARDWARE mode
echo Starting POSSUM with REAL hardware devices...
echo Make sure your devices (POSKeyboard1, ToneIndicator1, Keylock1) are connected!
echo.
echo Press Ctrl+C to stop the server
echo.

REM IMPORTANT: Do NOT set SIMULATION_MODE, or set it to false
set SIMULATION_MODE=false
gradlew.bat bootRun -Dposkeyboard.enabled=true -Dtoneindicator.enabled=true -Dkeylock.enabled=true

echo.
echo Server stopped.
pause

