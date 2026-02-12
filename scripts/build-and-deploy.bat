@echo off
REM ============================================================================
REM URSA JavaPOS Middleware - Build and Deploy Script (Windows)
REM ============================================================================
REM Builds POSSUM from source and deploys to C:\target\possum.
REM Run this BEFORE install-service.bat on a new register.
REM
REM Usage:
REM   build-and-deploy.bat           (build + deploy)
REM   build-and-deploy.bat --skip-build   (deploy only, use existing build)
REM
REM Prerequisites:
REM   - Java 17 installed
REM   - Git repo cloned with source code
REM ============================================================================

setlocal enabledelayedexpansion

REM ---- Configuration ----
set POSSUM_HOME=C:\target\possum
set SCRIPT_DIR=%~dp0
set REPO_DIR=%SCRIPT_DIR%..

REM ---- Parse arguments ----
set SKIP_BUILD=0
if "%~1"=="--skip-build" set SKIP_BUILD=1

echo.
echo ============================================================
echo  URSA JavaPOS Middleware - Build and Deploy
echo ============================================================
echo.
echo  Repository:  %REPO_DIR%
echo  Deploy to:   %POSSUM_HOME%
echo  Skip build:  %SKIP_BUILD%
echo.

REM ---- Verify Java is available ----
java -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ERROR: Java not found on PATH. Install Java 17 first.
    pause
    exit /b 1
)

REM ---- Build ----
if %SKIP_BUILD% equ 0 (
    echo.
    echo [1/3] Building POSSUM...
    echo ============================================================
    cd /d "%REPO_DIR%"

    REM Check for gradlew
    if not exist "%REPO_DIR%\gradlew.bat" (
        echo ERROR: gradlew.bat not found in %REPO_DIR%
        echo Are you in the POSSUM repository?
        pause
        exit /b 1
    )

    call gradlew.bat clean bootJar -x test
    if %ERRORLEVEL% neq 0 (
        echo.
        echo ERROR: Build failed. Check output above for errors.
        pause
        exit /b 1
    )
    echo.
    echo Build successful.
) else (
    echo.
    echo [1/3] Skipping build (--skip-build)
)

REM ---- Find the built JAR ----
set JAR_FILE=
for %%f in ("%REPO_DIR%\build\libs\PossumDeviceManager-*.jar") do (
    REM Skip the -plain jar if it exists
    echo %%~nf | findstr /i "plain" >nul
    if !ERRORLEVEL! neq 0 (
        set "JAR_FILE=%%f"
    )
)

if not defined JAR_FILE (
    echo ERROR: Could not find built JAR in %REPO_DIR%\build\libs\
    echo Run the build first, or check for build errors.
    pause
    exit /b 1
)

echo.
echo  Found JAR: %JAR_FILE%

REM ---- Create deployment directories ----
echo.
echo [2/3] Creating deployment directories...
echo ============================================================

if not exist "%POSSUM_HOME%" mkdir "%POSSUM_HOME%"
if not exist "%POSSUM_HOME%\config" mkdir "%POSSUM_HOME%\config"
if not exist "%POSSUM_HOME%\logs" mkdir "%POSSUM_HOME%\logs"
if not exist "%POSSUM_HOME%\externalLib" mkdir "%POSSUM_HOME%\externalLib"

REM ---- Deploy files ----
echo.
echo [3/3] Deploying to %POSSUM_HOME%...
echo ============================================================

REM Copy JAR (renamed to possum.jar)
echo  Copying possum.jar...
copy /Y "%JAR_FILE%" "%POSSUM_HOME%\possum.jar" >nul
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to copy JAR file.
    echo Is the service running? Stop it first: manage-service.bat stop
    pause
    exit /b 1
)

REM Copy config files
echo  Copying config files...
if exist "%REPO_DIR%\src\main\resources\devcon.xml" (
    copy /Y "%REPO_DIR%\src\main\resources\devcon.xml" "%POSSUM_HOME%\config\devcon.xml" >nul
)
if exist "%REPO_DIR%\src\main\resources\ECIEncoding.csv" (
    copy /Y "%REPO_DIR%\src\main\resources\ECIEncoding.csv" "%POSSUM_HOME%\config\ECIEncoding.csv" >nul
)
if exist "%REPO_DIR%\src\main\resources\LabelIdentifiers.csv" (
    copy /Y "%REPO_DIR%\src\main\resources\LabelIdentifiers.csv" "%POSSUM_HOME%\config\LabelIdentifiers.csv" >nul
)
if exist "%REPO_DIR%\src\main\resources\IHSParser.csv" (
    copy /Y "%REPO_DIR%\src\main\resources\IHSParser.csv" "%POSSUM_HOME%\config\IHSParser.csv" >nul
)
if exist "%REPO_DIR%\src\main\resources\logback-spring.xml" (
    copy /Y "%REPO_DIR%\src\main\resources\logback-spring.xml" "%POSSUM_HOME%\config\logback-spring.xml" >nul
)
if exist "%REPO_DIR%\src\main\resources\application.properties" (
    copy /Y "%REPO_DIR%\src\main\resources\application.properties" "%POSSUM_HOME%\config\application.properties" >nul
)
if exist "%REPO_DIR%\src\main\resources\application-local.properties" (
    copy /Y "%REPO_DIR%\src\main\resources\application-local.properties" "%POSSUM_HOME%\config\application-local.properties" >nul
)
if exist "%REPO_DIR%\src\main\resources\possum-config.yml" (
    REM Only copy possum-config.yml if one doesn't already exist (don't overwrite custom config)
    if not exist "%POSSUM_HOME%\config\possum-config.yml" (
        copy /Y "%REPO_DIR%\src\main\resources\possum-config.yml" "%POSSUM_HOME%\config\possum-config.yml" >nul
        echo  Copied default possum-config.yml (new install)
    ) else (
        echo  Keeping existing possum-config.yml (not overwritten)
    )
)

REM Copy service scripts
echo  Copying service scripts...
copy /Y "%SCRIPT_DIR%possum-service-run.bat" "%POSSUM_HOME%\possum-service-run.bat" >nul
copy /Y "%SCRIPT_DIR%install-service.bat" "%POSSUM_HOME%\install-service.bat" >nul
copy /Y "%SCRIPT_DIR%uninstall-service.bat" "%POSSUM_HOME%\uninstall-service.bat" >nul
copy /Y "%SCRIPT_DIR%manage-service.bat" "%POSSUM_HOME%\manage-service.bat" >nul

REM Copy NSSM
if exist "%SCRIPT_DIR%nssm.exe" (
    copy /Y "%SCRIPT_DIR%nssm.exe" "%POSSUM_HOME%\nssm.exe" >nul
    echo  Copied nssm.exe
)

echo.
echo ============================================================
echo  Build and Deploy Complete!
echo ============================================================
echo.
echo  Deployed to: %POSSUM_HOME%
echo.
echo  Directory contents:
echo    possum.jar            - Application JAR
echo    config\               - Configuration files
echo    logs\                 - Log files (created on first run)
echo    externalLib\          - External JARs (if any)
echo    possum-service-run.bat - Service runner script
echo    install-service.bat   - Service installer (run as Admin)
echo    uninstall-service.bat - Service uninstaller
echo    manage-service.bat    - Service start/stop/restart/status
echo    nssm.exe              - Service manager
echo.
echo  Next steps:
echo    1. Run as Administrator: %POSSUM_HOME%\install-service.bat
echo    2. Verify: http://localhost:8080/v1/health
echo    3. Discovery: http://localhost:8080/v1/discovery
echo.

pause
exit /b 0
