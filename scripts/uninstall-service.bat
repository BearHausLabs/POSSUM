@echo off
REM ============================================================================
REM URSA JavaPOS Middleware - Windows Service Uninstaller (NSSM)
REM ============================================================================
REM Stops and removes the POSSUM Windows service.
REM Must be run as Administrator.
REM ============================================================================

setlocal enabledelayedexpansion

set SERVICE_NAME=URSA JavaPOS Middleware

REM Determine script directory
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
        echo Place nssm.exe in %SCRIPT_DIR% or add it to PATH.
        echo.
        pause
        exit /b 1
    )
)

echo.
echo ============================================================
echo  URSA JavaPOS Middleware - Service Uninstaller
echo ============================================================
echo.

REM ---- Check if service exists ----
"%NSSM%" status "%SERVICE_NAME%" >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo Service "%SERVICE_NAME%" is not installed. Nothing to do.
    echo.
    pause
    exit /b 0
)

REM ---- Stop the service if running ----
echo Stopping service "%SERVICE_NAME%"...
"%NSSM%" stop "%SERVICE_NAME%" >nul 2>&1
timeout /t 5 /nobreak >nul
echo Service stopped.

REM ---- Remove the service ----
echo Removing service...
"%NSSM%" remove "%SERVICE_NAME%" confirm
if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: Failed to remove service. It may require a reboot.
    echo Try running: sc delete "URSA JavaPOS Middleware"
    echo.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo  Service "%SERVICE_NAME%" has been removed successfully.
echo ============================================================
echo.
echo  Log files remain at C:\target\possum\logs\ (delete manually if desired).
echo.

pause
exit /b 0
