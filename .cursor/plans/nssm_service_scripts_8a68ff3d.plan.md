---
name: NSSM Service Scripts
overview: Create Windows (NSSM) and Linux (systemd) service installation scripts for the "URSA JavaPOS Middleware" service, placed directly in the POSSUM `scripts/` folder alongside the existing devicestarter.sh. The previous WinSW approach had persistent service ID conflicts; NSSM is simpler and more reliable.
todos:
  - id: service-run-bat
    content: Create scripts/possum-service-run.bat -- non-interactive wrapper based on proven C:\target\possum\start-possum-service.bat
    status: completed
  - id: install-bat
    content: Create scripts/install-service.bat -- NSSM install + configure + start (Windows)
    status: completed
  - id: uninstall-bat
    content: Create scripts/uninstall-service.bat -- NSSM stop + remove (Windows)
    status: completed
  - id: manage-bat
    content: Create scripts/manage-service.bat -- start/stop/restart/status utility (Windows)
    status: completed
  - id: systemd-unit
    content: Create scripts/ursa-pos-middleware.service -- systemd unit file (Linux)
    status: completed
  - id: linux-install
    content: Create scripts/install-service.sh -- copy unit, enable, start (Linux)
    status: completed
  - id: linux-uninstall
    content: Create scripts/uninstall-service.sh -- stop, disable, remove (Linux)
    status: completed
  - id: gitignore
    content: Update .gitignore to exclude nssm.exe binary
    status: completed
isProject: false
---

# URSA JavaPOS Middleware Service Scripts

## Background

The previous attempt used WinSW (`possum-service.xml` at `C:\target\possum`) and repeatedly failed with "A service with ID 'URSA-POSSUM' already exists" errors, as seen in `C:\target\possum\logs\possum-service.wrapper.log`. NSSM is a better approach -- it manages the service lifecycle externally and handles stdout/stderr redirection, rotation, and restart policies natively.

The working Java launch command (from [`C:\target\possum\start-possum-service.bat`]) is:

```batch
"%JAVA_HOME%\bin\java.exe" -cp "%CP%" ^
  -Djava.library.path="%LIB_PATH%" ^
  -Djpos.tracing=ON ^
  -Djpos.config.populatorFile=config\devcon.xml ^
  -Dloader.main=com.target.devicemanager.DeviceMain ^
  -Dspring.profiles.active=local ^
  org.springframework.boot.loader.launch.PropertiesLauncher
```

## File Structure

All new files go directly into `[scripts/](scripts/)` alongside the existing `devicestarter.sh`:

```
scripts/
  devicestarter.sh                (existing - Linux manual start)
  possum-service-run.bat          (wrapper script NSSM executes)
  install-service.bat             (Windows - installs service via NSSM)
  uninstall-service.bat           (Windows - removes service via NSSM)
  manage-service.bat              (Windows - start/stop/restart/status)
  ursa-pos-middleware.service      (Linux - systemd unit file)
  install-service.sh              (Linux - installs systemd unit)
  uninstall-service.sh            (Linux - removes systemd unit)
  nssm.exe                        (user downloads - NOT committed to git)
```

## Windows Scripts (NSSM)

### 1. `scripts/possum-service-run.bat`

The non-interactive wrapper script that NSSM will execute. Based on the proven working configuration from `C:\target\possum\start-possum-service.bat`:

- Sets `JAVA_HOME`, `POSSUM_HOME`, `SPRING_PROFILES_ACTIVE`, `CORS_ORIGINS`
- Builds full classpath (POSSUM jar + Toshiba JavaPOS JARs + externalLib)
- Sets native library path to `C:\POS\bin` (the critical fix)
- Starts Toshiba AIP daemons if not running
- Launches Java with PropertiesLauncher
- **No `pause` or interactive prompts** -- critical for NSSM compatibility
- Exits with the Java process exit code

### 2. `scripts/install-service.bat`

Requires **elevation** (Run as Administrator). Steps:

- Checks for `nssm.exe` in same directory or on PATH
- Removes any leftover service with same name (clean install)
- Runs `nssm install "URSA JavaPOS Middleware" <path>\possum-service-run.bat`
- Configures NSSM parameters:
  - `AppDirectory` = `C:\target\possum`
  - `DisplayName` = `URSA JavaPOS Middleware`
  - `Description` = `URSA JavaPOS Middleware - JavaPOS REST API service for POS peripherals`
  - `Start` = `SERVICE_AUTO_START` (starts on boot)
  - `AppStdout` = `C:\target\possum\logs\service-stdout.log`
  - `AppStderr` = `C:\target\possum\logs\service-stderr.log`
  - `AppRotateFiles` = `1` (enable log rotation)
  - `AppRotateBytes` = `10485760` (10MB per log file)
  - `AppRestartDelay` = `5000` (5s restart delay on crash)
  - `AppExit Default` = `Restart` (auto-restart on failure)
  - `AppEnvironmentExtra` = environment variables (JAVA_HOME, SPRING_PROFILES_ACTIVE, CORS_ORIGINS)
- Starts the service
- Verifies it is running

### 3. `scripts/uninstall-service.bat`

- Stops the service if running
- Runs `nssm remove "URSA JavaPOS Middleware" confirm`
- Confirms removal

### 4. `scripts/manage-service.bat`

Utility script accepting `start`, `stop`, `restart`, `status` arguments:

- `manage-service.bat start` -- starts the service
- `manage-service.bat stop` -- stops gracefully
- `manage-service.bat restart` -- restarts
- `manage-service.bat status` -- shows current state via `nssm status`

## Linux Scripts (systemd)

### 5. `scripts/ursa-pos-middleware.service`

systemd unit file:

- `[Unit]` -- Description, After=network.target
- `[Service]` -- Type=simple, User/Group from current user, WorkingDirectory=/opt/target/possum
- `ExecStart` invokes `/opt/target/possum/devicestarter.sh` (the existing `[scripts/devicestarter.sh](scripts/devicestarter.sh)` deployed to that path)
- Environment variables: `SPRING_PROFILES_ACTIVE`, `POSSUM_LOG_PATH`
- Restart=on-failure, RestartSec=5
- StandardOutput/StandardError to journal
- `[Install]` -- WantedBy=multi-user.target

### 6. `scripts/install-service.sh`

- Copies the `.service` file to `/etc/systemd/system/`
- Runs `systemctl daemon-reload`
- Runs `systemctl enable ursa-pos-middleware`
- Runs `systemctl start ursa-pos-middleware`
- Shows status

### 7. `scripts/uninstall-service.sh`

- Stops the service
- Disables the service
- Removes the `.service` file
- Runs `systemctl daemon-reload`

## .gitignore Update

Add `scripts/nssm.exe` to `.gitignore` so the NSSM binary is not committed. The install script will print instructions to download NSSM if it is not found.

## Key Differences from Previous WinSW Attempt

- NSSM manages the process externally (no XML config embedded in the app)
- Clean install/uninstall with explicit `nssm remove` avoids the "service already exists" errors
- NSSM handles stdout/stderr capture and log rotation natively
- No need for a service wrapper DLL -- just wraps the batch file directly

