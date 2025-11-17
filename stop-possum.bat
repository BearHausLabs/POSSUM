@echo off
REM ========================================
REM  POSSUM - Stop Server
REM ========================================

echo.
echo ========================================
echo   Stopping POSSUM Server
echo ========================================
echo.

REM Change to the correct directory
cd /d "%~dp0"

REM Stop Gradle daemon (which stops bootRun)
echo Stopping Gradle daemon...
call gradlew.bat --stop

REM Wait a moment
timeout /t 2 /nobreak >nul

REM Double-check and kill any remaining Java processes related to POSSUM
echo Checking for remaining processes...
for /f "tokens=2" %%a in ('tasklist /FI "IMAGENAME eq java.exe" /FO LIST ^| find "PID:"') do (
    echo Terminating process %%a
    taskkill /F /PID %%a >nul 2>&1
)

echo.
echo [DONE] POSSUM server stopped.
echo.
pause

