@echo off
REM ============================================================================
REM URSA JavaPOS Middleware - Service Management Script
REM ============================================================================
REM Usage:  manage-service.bat [start|stop|restart|status]
REM Must be run as Administrator (except for status).
REM ============================================================================

setlocal enabledelayedexpansion

set SERVICE_NAME=URSA JavaPOS Middleware

REM Determine script directory
set SCRIPT_DIR=%~dp0

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
        exit /b 1
    )
)

REM ---- Parse command ----
if "%~1"=="" goto :usage

if /i "%~1"=="start"   goto :do_start
if /i "%~1"=="stop"    goto :do_stop
if /i "%~1"=="restart" goto :do_restart
if /i "%~1"=="status"  goto :do_status
goto :usage

REM ---- Start ----
:do_start
echo Starting "%SERVICE_NAME%"...
"%NSSM%" start "%SERVICE_NAME%"
if %ERRORLEVEL% neq 0 (
    echo Failed to start service. Check logs at C:\target\possum\logs\
)
goto :eof

REM ---- Stop ----
:do_stop
echo Stopping "%SERVICE_NAME%"...
"%NSSM%" stop "%SERVICE_NAME%"
if %ERRORLEVEL% neq 0 (
    echo Failed to stop service. It may not be running.
)
goto :eof

REM ---- Restart ----
:do_restart
echo Restarting "%SERVICE_NAME%"...
"%NSSM%" restart "%SERVICE_NAME%"
if %ERRORLEVEL% neq 0 (
    echo Failed to restart service. Trying stop then start...
    "%NSSM%" stop "%SERVICE_NAME%" >nul 2>&1
    timeout /t 3 /nobreak >nul
    "%NSSM%" start "%SERVICE_NAME%"
)
goto :eof

REM ---- Status ----
:do_status
echo.
echo Service: %SERVICE_NAME%
echo.
"%NSSM%" status "%SERVICE_NAME%"
echo.
echo Log files: C:\target\possum\logs\
echo Endpoint:  http://localhost:8080
echo Health:    http://localhost:8080/v1/health
echo.
goto :eof

REM ---- Usage ----
:usage
echo.
echo ============================================================
echo  URSA JavaPOS Middleware - Service Manager
echo ============================================================
echo.
echo  Usage: %~nx0 [command]
echo.
echo  Commands:
echo    start    - Start the service
echo    stop     - Stop the service
echo    restart  - Restart the service
echo    status   - Show service status
echo.
echo  Examples:
echo    %~nx0 start
echo    %~nx0 status
echo.
exit /b 1
