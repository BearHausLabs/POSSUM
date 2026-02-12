---
name: Fix Keylock Claim
overview: Fix the Keylock device by adding a "claimless" connection mode to POSSUM, mirroring the Toshiba demo pattern which does NOT call `claim()` on the Keylock. The Toshiba UPOS Management Service already owns the claim, causing Error 106.
todos:
  - id: skip-claim-connector
    content: "Add skipClaim flag to DeviceConnector: new field, constructor param, skip claim() in connect(), skip release() in clearDeviceCache(), add getter"
    status: completed
  - id: skip-claim-dynamic
    content: "Update DynamicDevice: skip getClaimed() check in isConnected() and skip release() in disconnect() when skipClaim is true"
    status: completed
  - id: keylock-config
    content: Update KeylockConfig to pass skipClaim=true for the Keylock DeviceConnector
    status: completed
  - id: build-deploy
    content: Build, deploy JAR, restart service, and verify keylock position changes work
    status: completed
isProject: false
---

# Fix Keylock: Skip Claim for Shared Devices

## Root Cause

The Toshiba demo code reveals the critical difference:

**Toshiba Demo (works):** `open()` -> `addStatusUpdateListener()` -> `setDeviceEnabled(true)` -- **no `claim()` at all**

**POSSUM (broken):** `open()` -> `claim()` -> FAILS with Error 106 because Toshiba UPOS Management Service already holds the claim

The Keylock is a read-only shared device. It does not need exclusive access. POSSUM's `DeviceConnector.connect()` at [DeviceConnector.java](C:\Users\regadmin\Documents\GitHub\POSSUM\src\main\java\com\target\devicemanager\common\DeviceConnector.java) line 125 always calls `device.claim()`, and `DynamicDevice.isConnected()` at [DynamicDevice.java](C:\Users\regadmin\Documents\GitHub\POSSUM\src\main\java\com\target\devicemanager\common\DynamicDevice.java) line 84 always checks `device.getClaimed()`.

## Connection Flow Comparison

```mermaid
sequenceDiagram
    participant App
    participant JPOS
    Note over App,JPOS: Toshiba Demo (Keylock)
    App->>JPOS: open(logicalName)
    App->>JPOS: addStatusUpdateListener()
    App->>JPOS: setPowerNotify(ENABLED)
    App->>JPOS: setDeviceEnabled(true)
    Note over App,JPOS: POSSUM Current (ALL devices)
    App->>JPOS: open(logicalName)
    App->>JPOS: claim(30000)
    JPOS-->>App: ERROR 106 - already claimed
    Note over App: Never reaches enable
```

## Changes

### 1. `DeviceConnector.java` - Add `skipClaim` flag

- Add a `boolean skipClaim` field (default `false` for backward compatibility)
- Add a new constructor parameter for it
- In `connect()` method (line 116-148): skip `device.claim()` when `skipClaim` is true
- In `clearDeviceCache()` (line 96-114): skip `device.release()` when `skipClaim` is true
- Add a `isSkipClaim()` getter so `DynamicDevice` can check it

### 2. `DynamicDevice.java` - Handle claimless mode

- In `isConnected()` (line 77-96): when `skipClaim` is true, skip the `device.getClaimed()` check (line 84). Instead, just verify device state is IDLE or BUSY.
- In `disconnect()` (line 60-75): skip `device.release()` when `skipClaim` is true (no claim = no release)

### 3. `KeylockConfig.java` - Enable claimless mode for Keylock

- Pass `skipClaim: true` when constructing the `DeviceConnector` for the Keylock device (line 43)

### 4. `KeylockDevice.java` - Also query position from API response  

- Keep the live-read `getKeyPosition()` change already made (reads `keylock.getKeyPosition()` directly)
- Remove accumulated debug instrumentation after verification

### 5. Build and deploy

- Rebuild POSSUM JAR
- Deploy to `C:\target\possum\possum.jar`
- Restart service
