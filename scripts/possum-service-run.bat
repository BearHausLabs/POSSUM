@echo off
REM ============================================================================
REM URSA JavaPOS Middleware - Service Runner Script
REM ============================================================================
REM This script is executed by NSSM to run POSSUM as a Windows service.
REM It is NON-INTERACTIVE -- no pause, no prompts, no user input required.
REM Based on the proven working configuration from start-possum-service.bat.
REM ============================================================================

set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot
set POSSUM_HOME=C:\target\possum
set SPRING_PROFILES_ACTIVE=local

REM CORS configuration for Angular backoffice and mobile devices
set CORS_ORIGINS=http://localhost:4200,http://localhost:3000,http://10.11.35.100:4200,http://10.11.35.5:8888,http://10.11.35.100:8888

REM Build classpath - matches TOSHIBA_JPOS_CLASSPATH exactly, plus POSSUM
set CP=%POSSUM_HOME%\possum.jar
set CP=%CP%;C:\POS\JavaPOS\Config
set CP=%CP%;C:\POS\JavaPOS
set CP=%CP%;C:\POS\JavaPOS\Lib\tgcsjavapos.jar
set CP=%CP%;C:\POS\JavaPOS\Lib\jpos114.jar
set CP=%CP%;C:\POS\JavaPOS\Lib\jpos_sysmgmt.jar
set CP=%CP%;C:\POS\JavaPOS\Lib\xml-apis.jar
set CP=%CP%;C:\POS\JavaPOS\Lib\xercesImpl.jar
set CP=%CP%;C:\POS\JavaPOS\Lib\jsr80.jar
set CP=%CP%;C:\POS\JavaPOS\Lib\jsr80_ri_windows.jar
set CP=%CP%;C:\POS\JavaPOS\Lib\jsr80_ri.jar
set CP=%CP%;C:\POS\JavaPOS\rxtx\RXTXcomm.jar
set CP=%CP%;%POSSUM_HOME%\externalLib\*

REM Build library path for native libraries
REM CRITICAL: Must point to C:\POS\bin (where Toshiba DLLs actually reside)
set LIB_PATH=%POSSUM_HOME%\externalLib;C:\POS\bin;C:\POS\JavaPOS\Lib

REM Create logs directory if it doesn't exist
if not exist "%POSSUM_HOME%\logs" mkdir "%POSSUM_HOME%\logs"

REM Log service startup
echo [%DATE% %TIME%] URSA JavaPOS Middleware Service Starting... >> "%POSSUM_HOME%\logs\service-startup.log"

REM Sync Toshiba jpos.xml to POSSUM devcon.xml
REM DevCat auto-configures jpos.xml with current device entries; POSSUM reads devcon.xml
if not exist "%POSSUM_HOME%\config" mkdir "%POSSUM_HOME%\config"
if exist "C:\POS\JavaPOS\jpos.xml" (
    copy /Y "C:\POS\JavaPOS\jpos.xml" "%POSSUM_HOME%\config\devcon.xml" >nul
    echo [%DATE% %TIME%] Synced jpos.xml to config\devcon.xml >> "%POSSUM_HOME%\logs\service-startup.log"
) else (
    echo [%DATE% %TIME%] WARNING: C:\POS\JavaPOS\jpos.xml not found, using existing devcon.xml >> "%POSSUM_HOME%\logs\service-startup.log"
)

REM Start Toshiba AIP daemons if not already running
tasklist /FI "IMAGENAME eq aipctrld.exe" 2>nul | find /i "aipctrld.exe" >nul
if errorlevel 1 (
    echo [%DATE% %TIME%] Starting Toshiba AIP daemons... >> "%POSSUM_HOME%\logs\service-startup.log"
    start "" /B "C:\POS\bin\aipctrld.exe"
    start "" /B "C:\POS\bin\aipstartd.exe"
    start "" /B "C:\POS\bin\aiptraced.exe"
    start "" /B "C:\POS\bin\aip4750cd.exe"
    start "" /B "C:\POS\bin\aipanposd.exe"
    REM Wait for daemons to initialize (non-interactive, no /nobreak needed for service)
    ping -n 6 127.0.0.1 >nul
) else (
    echo [%DATE% %TIME%] Toshiba AIP daemons already running. >> "%POSSUM_HOME%\logs\service-startup.log"
)

REM Change to POSSUM directory
cd /d "%POSSUM_HOME%"

echo [%DATE% %TIME%] Launching POSSUM Java application... >> "%POSSUM_HOME%\logs\service-startup.log"
echo [%DATE% %TIME%] POSSUM will be available at: http://localhost:8080 >> "%POSSUM_HOME%\logs\service-startup.log"

REM Launch POSSUM using PropertiesLauncher (same as devicestarter.sh)
REM NSSM captures stdout/stderr automatically -- no redirection needed here
"%JAVA_HOME%\bin\java.exe" ^
  -cp "%CP%" ^
  -Djava.library.path="%LIB_PATH%" ^
  -Djpos.tracing=ON ^
  -Djpos.config.populatorFile=config\devcon.xml ^
  -Dloader.main=com.target.devicemanager.DeviceMain ^
  -Dspring.profiles.active=%SPRING_PROFILES_ACTIVE% ^
  org.springframework.boot.loader.launch.PropertiesLauncher

REM If Java exits, log it
echo [%DATE% %TIME%] POSSUM Java process exited with code %ERRORLEVEL% >> "%POSSUM_HOME%\logs\service-startup.log"
exit /b %ERRORLEVEL%
