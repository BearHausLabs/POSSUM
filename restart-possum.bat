@echo off
REM ========================================
REM  POSSUM - Restart Server
REM ========================================

echo.
echo ========================================
echo   Restarting POSSUM Server
echo ========================================
echo.

REM Change to the correct directory
cd /d "%~dp0"

REM Stop the server
echo [Step 1/3] Stopping server...
call stop-possum.bat

REM Wait for complete shutdown
echo.
echo [Step 2/3] Waiting for shutdown...
timeout /t 3 /nobreak >nul

REM Rebuild the application to pick up config changes
echo.
echo [Step 3/3] Rebuilding and starting server...
echo.
call gradlew.bat clean build

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Build failed! Fix errors before starting.
    echo.
    pause
    exit /b 1
)

echo.
echo Build successful! Starting server...
echo.

REM Start the server
set SIMULATION_MODE=true
start "POSSUM Server" cmd /k "gradlew.bat bootRun -Dposkeyboard.enabled=true -Dtoneindicator.enabled=true -Dkeylock.enabled=true"

echo.
echo [DONE] POSSUM server restarted in new window.
echo Check the new window for server status.
echo.
pause

