@echo off
REM ============================================================================
REM URSA JavaPOS Middleware - Windows Service Installer (NSSM)
REM ============================================================================
REM Installs POSSUM as a Windows service using NSSM (Non-Sucking Service Manager).
REM Must be run as Administrator.
REM
REM Prerequisites:
REM   - Download nssm.exe from https://nssm.cc/download (v2.24 recommended)
REM   - Place nssm.exe in the same directory as this script (scripts\)
REM   - POSSUM deployed at C:\target\possum with possum.jar
REM   - Java 17 installed at JAVA_HOME path below
REM ============================================================================

setlocal enabledelayedexpansion

set SERVICE_NAME=URSA JavaPOS Middleware
set POSSUM_HOME=C:\target\possum
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot

REM Determine script directory (where nssm.exe and possum-service-run.bat are)
set SCRIPT_DIR=%~dp0

REM ---- Check for Administrator privileges ----
net session >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: This script must be run as Administrator.
    echo Right-click and select "Run as administrator".
    echo.
    pause
    exit /b 1
)

REM ---- Locate NSSM ----
set NSSM=
if exist "%SCRIPT_DIR%nssm.exe" (
    set "NSSM=%SCRIPT_DIR%nssm.exe"
) else (
    where nssm.exe >nul 2>&1
    if !ERRORLEVEL! equ 0 (
        set "NSSM=nssm.exe"
    ) else (
        echo.
        echo ERROR: nssm.exe not found.
        echo.
        echo Please download NSSM from: https://nssm.cc/download
        echo   1. Download nssm-2.24.zip
        echo   2. Extract the appropriate nssm.exe for your architecture:
        echo      - 64-bit Windows: nssm-2.24\win64\nssm.exe
        echo      - 32-bit Windows: nssm-2.24\win32\nssm.exe
        echo   3. Copy nssm.exe to: %SCRIPT_DIR%
        echo   4. Re-run this script.
        echo.
        pause
        exit /b 1
    )
)

echo.
echo ============================================================
echo  URSA JavaPOS Middleware - Service Installer
echo ============================================================
echo.
echo  NSSM:         %NSSM%
echo  Service Name: %SERVICE_NAME%
echo  POSSUM Home:  %POSSUM_HOME%
echo  Java Home:    %JAVA_HOME%
echo  Runner:       %SCRIPT_DIR%possum-service-run.bat
echo.

REM ---- Verify prerequisites ----
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: Java not found at %JAVA_HOME%\bin\java.exe
    pause
    exit /b 1
)

if not exist "%SCRIPT_DIR%possum-service-run.bat" (
    echo ERROR: possum-service-run.bat not found in %SCRIPT_DIR%
    pause
    exit /b 1
)

if not exist "%POSSUM_HOME%\possum.jar" (
    echo WARNING: possum.jar not found at %POSSUM_HOME%\possum.jar
    echo          The service will fail to start without it.
    echo.
)

REM ---- Remove existing service if present (clean install) ----
echo Checking for existing service...
"%NSSM%" status "%SERVICE_NAME%" >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo Found existing service. Stopping and removing...
    "%NSSM%" stop "%SERVICE_NAME%" >nul 2>&1
    timeout /t 3 /nobreak >nul
    "%NSSM%" remove "%SERVICE_NAME%" confirm >nul 2>&1
    timeout /t 2 /nobreak >nul
    echo Previous service removed.
) else (
    echo No existing service found. Proceeding with fresh install.
)

REM ---- Create logs directory ----
if not exist "%POSSUM_HOME%\logs" mkdir "%POSSUM_HOME%\logs"

REM ---- Install the service ----
echo.
echo Installing service...
"%NSSM%" install "%SERVICE_NAME%" "%SCRIPT_DIR%possum-service-run.bat"
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to install service.
    pause
    exit /b 1
)

REM ---- Configure service parameters ----
echo Configuring service parameters...

REM Display name and description
"%NSSM%" set "%SERVICE_NAME%" DisplayName "%SERVICE_NAME%"
"%NSSM%" set "%SERVICE_NAME%" Description "Allows URSA Retail Suite to connect to POS peripherals via JavaPOS. Powered by POSSUM."

REM Working directory
"%NSSM%" set "%SERVICE_NAME%" AppDirectory "%POSSUM_HOME%"

REM Startup type: Automatic
"%NSSM%" set "%SERVICE_NAME%" Start SERVICE_AUTO_START

REM Stdout and Stderr log files
"%NSSM%" set "%SERVICE_NAME%" AppStdout "%POSSUM_HOME%\logs\service-stdout.log"
"%NSSM%" set "%SERVICE_NAME%" AppStderr "%POSSUM_HOME%\logs\service-stderr.log"

REM Log file rotation (rotate when files exceed 10MB)
"%NSSM%" set "%SERVICE_NAME%" AppRotateFiles 1
"%NSSM%" set "%SERVICE_NAME%" AppRotateOnline 1
"%NSSM%" set "%SERVICE_NAME%" AppRotateBytes 10485760

REM Restart on failure with 5-second delay
"%NSSM%" set "%SERVICE_NAME%" AppRestartDelay 5000

REM Auto-restart on exit (any exit code)
"%NSSM%" set "%SERVICE_NAME%" AppExit Default Restart

REM Graceful shutdown: send Ctrl+C, wait 10 seconds before killing
"%NSSM%" set "%SERVICE_NAME%" AppStopMethodSkip 0
"%NSSM%" set "%SERVICE_NAME%" AppStopMethodConsole 10000
"%NSSM%" set "%SERVICE_NAME%" AppStopMethodWindow 10000
"%NSSM%" set "%SERVICE_NAME%" AppStopMethodThreads 10000

REM Environment variables
"%NSSM%" set "%SERVICE_NAME%" AppEnvironmentExtra ^
  JAVA_HOME=%JAVA_HOME% ^
  POSSUM_HOME=%POSSUM_HOME% ^
  SPRING_PROFILES_ACTIVE=local ^
  CORS_ORIGINS=http://localhost:4200,http://localhost:3000,http://10.11.35.100:4200,http://10.11.35.5:8888,http://10.11.35.100:8888

echo.
echo Service installed and configured successfully.
echo.

REM ---- Start the service ----
echo Starting service...
"%NSSM%" start "%SERVICE_NAME%"
if %ERRORLEVEL% neq 0 (
    echo WARNING: Service installed but failed to start.
    echo Check logs at: %POSSUM_HOME%\logs\
    echo.
    echo You can start it manually:
    echo   nssm start "%SERVICE_NAME%"
    echo   -- or --
    echo   net start "%SERVICE_NAME%"
    echo.
    pause
    exit /b 1
)

REM Wait briefly for startup
timeout /t 3 /nobreak >nul

REM ---- Verify service is running ----
echo.
echo Verifying service status...
"%NSSM%" status "%SERVICE_NAME%"
echo.

echo ============================================================
echo  Installation Complete
echo ============================================================
echo.
echo  Service "%SERVICE_NAME%" has been installed and started.
echo.
echo  Manage the service:
echo    scripts\manage-service.bat start
echo    scripts\manage-service.bat stop
echo    scripts\manage-service.bat restart
echo    scripts\manage-service.bat status
echo.
echo  Or use Windows Services (services.msc) to manage it.
echo.
echo  POSSUM endpoint: http://localhost:8080
echo  Health check:    http://localhost:8080/v1/health
echo  Logs:            %POSSUM_HOME%\logs\
echo.

pause
exit /b 0
