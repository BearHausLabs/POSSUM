---
name: JavaPOS device discovery
overview: "Implement the hybrid device discovery model: a JavaPOS Device Discovery Service that scans devcon.xml and tests devices (exposed via REST), plus an optional YAML workstation config file for explicit device mappings in production."
todos:
  - id: entities
    content: Create DeviceInfo, DeviceTestResult, DeviceDiscoveryResponse entity classes
    status: completed
  - id: discovery-service
    content: Create JavaPOSDeviceDiscoveryService with scanConfiguredDevices() and testDevice() methods
    status: completed
  - id: discovery-controller
    content: Create DeviceDiscoveryController with /v1/discovery, /v1/discovery/test, /v1/discovery/test/{logicalName} endpoints
    status: completed
  - id: workstation-config
    content: Create WorkstationConfig POJO with @ConfigurationProperties and default possum-config.yml
    status: completed
  - id: device-connector-update
    content: Add preferredLogicalName support to DeviceConnector.discoverConnectedDevice()
    status: completed
  - id: config-classes-update
    content: Update all 10 device Config classes to inject WorkstationConfig and pass explicit logical names
    status: completed
  - id: test-on-startup
    content: Add test-on-startup behavior triggered from DeviceMain after Spring context is ready
    status: completed
  - id: generate-config-endpoint
    content: Add POST /v1/discovery/generate-config endpoint to produce a possum-config.yml from discovered devices
    status: completed
isProject: false
---

# JavaPOS Device Discovery Service + Workstation Config

## Context

POSSUM already has runtime device discovery via `DeviceConnector.getLogicalNamesForDeviceType()`, which filters entries from `devcon.xml` by category and tries each logical name until one connects. The existing `/v1/health` endpoint reports which managers are online. What's missing:

- **No way to see all configured devices** (only those that successfully connected)
- **No device testing on demand** (only at startup)
- **No explicit device mapping** (every startup re-discovers, which is slow and non-deterministic)

## Architecture

```mermaid
flowchart TB
    subgraph config [Configuration Layer]
        devcon["devcon.xml (JPOS registry)"]
        yaml["possum-config.yml (workstation config)"]
    end
    subgraph discovery [Discovery Layer - NEW]
        svc["JavaPOSDeviceDiscoveryService"]
        ctrl["DeviceDiscoveryController"]
    end
    subgraph runtime [Runtime Layer - EXISTING]
        connector["DeviceConnector"]
        managers["Device Managers"]
        health["DeviceAvailabilityService"]
    end
    devcon -->|"parsed by JPOS"| svc
    yaml -->|"explicit mappings"| connector
    svc -->|"REST /v1/discovery"| ctrl
    devcon -->|"auto-discover"| connector
    connector --> managers
    managers --> health
```



## Part 1: Device Discovery Service

New files in `src/main/java/com/target/devicemanager/common/`:

### 1a. Entity classes

`**entities/DeviceInfo.java**` -- represents a single device entry from devcon.xml:

```java
public class DeviceInfo {
    private String logicalName;     // e.g. "CashDrawer0"
    private String category;        // e.g. "CashDrawer"
    private String productName;     // e.g. "CashDrawer"
    private String description;     // from <product description="..."/>
    private String vendorName;      // from <vendor name="..."/>
    private String version;         // from <jpos version="..."/>
    private String factoryClass;    // from <creation factoryClass="..."/>
    private String serviceClass;    // from <creation serviceClass="..."/>
    private Map<String, String> properties; // all <prop> entries
    // getters, constructor
}
```

`**entities/DeviceTestResult.java**` -- result of testing a single device:

```java
public class DeviceTestResult {
    private String logicalName;
    private String category;
    private boolean functional;
    private String message;
    private long testDurationMs;
    // getters, constructor
}
```

`**entities/DeviceDiscoveryResponse.java**` -- full discovery response:

```java
public class DeviceDiscoveryResponse {
    private List<DeviceInfo> configuredDevices;
    private List<DeviceTestResult> testResults; // null if tests not run
    private String configFile;                  // path to devcon.xml used
    private Instant discoveredAt;
}
```

### 1b. Service class

`**JavaPOSDeviceDiscoveryService.java**` -- the core discovery logic:

- `scanConfiguredDevices()` -- reads the JPOS `EntryRegistry` (same one used by `DeviceConnector`) and extracts all `JposEntry` data into `DeviceInfo` objects. No hardware interaction, just XML parsing.
- `testDevice(logicalName, category)` -- attempts open/claim/enable/disable/release/close cycle (same pattern as `DeviceConnector.connect()`) and returns a `DeviceTestResult`.
- `testAllDevices()` -- runs `scanConfiguredDevices()` then tests each one, returning a `DeviceDiscoveryResponse` with both scan and test results.

Leverage the existing `JposServiceLoader.getManager().getEntryRegistry()` to access parsed entries -- no need for custom XML parsing.

### 1c. REST Controller

`**DeviceDiscoveryController.java**` -- new controller with endpoints:

- `GET /v1/discovery` -- returns all configured devices (scan only, fast)
- `GET /v1/discovery/test` -- returns all configured devices with connectivity test results (slow, runs tests)
- `GET /v1/discovery/test/{logicalName}` -- tests a single device by logical name

Add to existing `DeviceAvailabilityController` or create as a separate controller (separate is cleaner).

## Part 2: Workstation Config File (YAML)

### 2a. Config POJO

`**configuration/WorkstationConfig.java**` -- maps the YAML structure:

```java
@ConfigurationProperties(prefix = "possum")
public class WorkstationConfig {
    private StoreInfo store;
    private WorkstationInfo workstation;
    private DeviceMappings devices;
    private DiscoverySettings discovery;
    // nested classes for each section
}
```

YAML structure (`possum-config.yml`):

```yaml
possum:
  store:
    id: "store-001"
    name: "Main Street Location"
  workstation:
    id: "lane-1"
    name: "Lane 1"
  devices:
    printer: "NCRPOSPrinter-7167"
    cashDrawer: "CashDrawer0"
    flatbedScanner: "Datalogic 8200 Scanner"
    handScanner: null
    scale: "Datalogic 8405 Scale"
    lineDisplay: "NCR 5977 LineDisplay USB"
    micr: "NCR 7167 MICR"
    keylock: "Keylock0"
    posKeyboard: "POSKeyboard"
    msr: "MSR"
    toneIndicator: "ToneIndicator4"
  discovery:
    enabled: true
    testOnStartup: true
    autoAdapt: false
```

Spring Boot automatically loads `possum-config.yml` if placed alongside `application.properties`, or it can be overridden with `--spring.config.additional-location=file:/path/to/possum-config.yml`.

### 2b. Modify DeviceConnector

Add an optional `preferredLogicalName` parameter:

```java
// Current: tries all matching logical names
boolean discoverConnectedDevice()

// New overload: tries preferred name first, falls back to discovery
boolean discoverConnectedDevice(String preferredLogicalName)
```

If `preferredLogicalName` is set and `autoAdapt` is false, it ONLY tries that name (fast, deterministic). If `autoAdapt` is true, it tries the preferred name first, then falls back to scanning all matching names.

### 2c. Modify Device Config classes

Each Config class (e.g., [KeylockConfig.java](src/main/java/com/target/devicemanager/components/keylock/KeylockConfig.java)) currently creates a `DeviceConnector` with just the device and registry. Inject `WorkstationConfig` and pass the explicit logical name if configured:

```java
// Before
new DeviceConnector<>(keylock, deviceRegistry)

// After (if config has explicit mapping)
new DeviceConnector<>(keylock, deviceRegistry, null, workstationConfig.getDevices().getKeylock())
```

### 2d. Startup test-on-startup behavior

If `discovery.testOnStartup` is true, run device tests after all managers are initialized and log results. This provides early visibility into hardware issues without requiring manual API calls.

## Part 3: Management API (bonus)

Add a `POST /v1/discovery/generate-config` endpoint that runs discovery + tests and returns a ready-to-use `possum-config.yml` file content. This lets admins:

1. Hit the endpoint on a freshly set up register
2. Get back a YAML file with all detected devices filled in
3. Deploy that YAML as the explicit config for production use

## Files to Create

- `src/main/java/com/target/devicemanager/common/entities/DeviceInfo.java`
- `src/main/java/com/target/devicemanager/common/entities/DeviceTestResult.java`
- `src/main/java/com/target/devicemanager/common/entities/DeviceDiscoveryResponse.java`
- `src/main/java/com/target/devicemanager/common/JavaPOSDeviceDiscoveryService.java`
- `src/main/java/com/target/devicemanager/common/DeviceDiscoveryController.java`
- `src/main/java/com/target/devicemanager/configuration/WorkstationConfig.java`
- `src/main/resources/possum-config.yml` (default/example)

## Files to Modify

- [DeviceConnector.java](src/main/java/com/target/devicemanager/common/DeviceConnector.java) -- add `preferredLogicalName` support
- All 10 device `*Config.java` classes -- inject `WorkstationConfig`, pass explicit name
- [DeviceMain.java](src/main/java/com/target/devicemanager/DeviceMain.java) -- optionally trigger test-on-startup

## Dependencies

No new dependencies needed. YAML parsing is already available via `jackson-dataformat-yaml:2.19.0` and `snakeyaml:2.4` in [build.gradle](build.gradle). The JPOS entry registry is already used by all Config classes.