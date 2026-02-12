package com.target.devicemanager.common;

import com.target.devicemanager.common.entities.DeviceDiscoveryResponse;
import com.target.devicemanager.common.entities.DeviceInfo;
import com.target.devicemanager.common.entities.DeviceTestResult;
import jpos.*;
import jpos.config.JposEntryRegistry;
import jpos.config.simple.SimpleEntry;
import jpos.loader.JposServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for discovering and testing JavaPOS devices configured in devcon.xml.
 *
 * This scans the JPOS EntryRegistry (populated from devcon.xml) to enumerate
 * all configured devices and optionally tests each one by attempting an
 * open/claim/enable/disable/release/close cycle.
 */
@Service
public class JavaPOSDeviceDiscoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaPOSDeviceDiscoveryService.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("Common", "DeviceDiscovery", LOGGER);
    private static final int CLAIM_TIMEOUT_MS = 5000;
    private static final int RETRY_REGISTRY_LOAD = 5;

    /**
     * Scans the JPOS EntryRegistry for all configured device entries.
     * This is a fast, read-only operation -- no hardware interaction.
     */
    public List<DeviceInfo> scanConfiguredDevices() {
        JposEntryRegistry registry = JposServiceLoader.getManager().getEntryRegistry();

        // Retry loading if registry is empty (same pattern as DeviceConnector)
        for (int i = 0; i < RETRY_REGISTRY_LOAD && registry.getSize() == 0; i++) {
            registry.load();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        ArrayList<SimpleEntry> entries = Collections.list(
                (Enumeration<SimpleEntry>) registry.getEntries());

        List<DeviceInfo> devices = entries.stream()
                .map(this::entryToDeviceInfo)
                .collect(Collectors.toList());

        log.success("Scanned " + devices.size() + " configured devices from JPOS registry", 5);
        return devices;
    }

    /**
     * Tests a single device by logical name and category.
     * Attempts open/claim/enable/disable/release/close to verify connectivity.
     */
    public DeviceTestResult testDevice(String logicalName, String category) {
        long startTime = System.currentTimeMillis();
        BaseJposControl control = createControlForCategory(category);

        if (control == null) {
            return new DeviceTestResult(logicalName, category, false,
                    "Unsupported device category: " + category,
                    System.currentTimeMillis() - startTime);
        }

        try {
            control.open(logicalName);
            try {
                control.claim(CLAIM_TIMEOUT_MS);
                try {
                    control.setDeviceEnabled(true);
                    String serviceDesc = "";
                    try {
                        serviceDesc = control.getDeviceServiceDescription();
                    } catch (Exception ignored) {
                        // Not all devices support this
                    }
                    control.setDeviceEnabled(false);
                    control.release();
                    control.close();

                    String message = "Device functional";
                    if (!serviceDesc.isEmpty()) {
                        message += ": " + serviceDesc;
                    }
                    return new DeviceTestResult(logicalName, category, true,
                            message, System.currentTimeMillis() - startTime);
                } catch (JposException e) {
                    safeClose(control);
                    return new DeviceTestResult(logicalName, category, false,
                            "Enable failed (error " + e.getErrorCode() + "): " + e.getMessage(),
                            System.currentTimeMillis() - startTime);
                }
            } catch (JposException e) {
                safeClose(control);
                return new DeviceTestResult(logicalName, category, false,
                        "Claim failed (error " + e.getErrorCode() + "): " + e.getMessage(),
                        System.currentTimeMillis() - startTime);
            }
        } catch (JposException e) {
            return new DeviceTestResult(logicalName, category, false,
                    "Open failed (error " + e.getErrorCode() + "): " + e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Discovers all configured devices and tests each one.
     * Returns a complete discovery response with scan + test results.
     */
    public DeviceDiscoveryResponse testAllDevices() {
        List<DeviceInfo> devices = scanConfiguredDevices();
        List<DeviceTestResult> results = new ArrayList<>();

        for (DeviceInfo device : devices) {
            log.success("Testing device: " + device.getLogicalName() + " (" + device.getCategory() + ")", 9);
            DeviceTestResult result = testDevice(device.getLogicalName(), device.getCategory());
            results.add(result);
            log.success("  Result: " + (result.isFunctional() ? "OK" : "FAIL") +
                    " - " + result.getMessage() + " (" + result.getTestDurationMs() + "ms)", 9);
        }

        String configFile = System.getProperty("jpos.config.populatorFile", "devcon.xml");
        return new DeviceDiscoveryResponse(devices, results, configFile, Instant.now());
    }

    /**
     * Returns a discovery response with scan results only (no tests).
     */
    public DeviceDiscoveryResponse scanOnly() {
        List<DeviceInfo> devices = scanConfiguredDevices();
        String configFile = System.getProperty("jpos.config.populatorFile", "devcon.xml");
        return new DeviceDiscoveryResponse(devices, null, configFile, Instant.now());
    }

    /**
     * Generates a YAML workstation config from discovered and tested devices.
     * For each device category, picks the first functional device found.
     */
    public String generateConfigYaml() {
        DeviceDiscoveryResponse discovery = testAllDevices();
        Map<String, String> categoryToLogicalName = new LinkedHashMap<>();

        if (discovery.getTestResults() != null) {
            for (DeviceTestResult result : discovery.getTestResults()) {
                if (result.isFunctional()) {
                    String category = result.getCategory();
                    // Only keep the first functional device per category
                    categoryToLogicalName.putIfAbsent(category, result.getLogicalName());
                }
            }
        }

        StringBuilder yaml = new StringBuilder();
        yaml.append("# Auto-generated POSSUM workstation config\n");
        yaml.append("# Generated: ").append(Instant.now()).append("\n");
        yaml.append("# Config source: ").append(discovery.getConfigFile()).append("\n\n");
        yaml.append("possum:\n");
        yaml.append("  store:\n");
        yaml.append("    id: \"store-001\"\n");
        yaml.append("    name: \"Auto-Discovered\"\n");
        yaml.append("  workstation:\n");
        yaml.append("    id: \"lane-1\"\n");
        yaml.append("    name: \"Lane 1\"\n");
        yaml.append("  devices:\n");

        appendDeviceMapping(yaml, "printer", categoryToLogicalName.get("POSPrinter"));
        appendDeviceMapping(yaml, "cashDrawer", categoryToLogicalName.get("CashDrawer"));
        appendDeviceMapping(yaml, "flatbedScanner", getCategoryWithFilter(discovery, "Scanner", "Flatbed"));
        appendDeviceMapping(yaml, "handScanner", getCategoryWithFilter(discovery, "Scanner", "HandScanner"));
        appendDeviceMapping(yaml, "scale", categoryToLogicalName.get("Scale"));
        appendDeviceMapping(yaml, "lineDisplay", categoryToLogicalName.get("LineDisplay"));
        appendDeviceMapping(yaml, "micr", categoryToLogicalName.get("MICR"));
        appendDeviceMapping(yaml, "keylock", categoryToLogicalName.get("Keylock"));
        appendDeviceMapping(yaml, "posKeyboard", categoryToLogicalName.get("POSKeyboard"));
        appendDeviceMapping(yaml, "msr", categoryToLogicalName.get("MSR"));
        appendDeviceMapping(yaml, "toneIndicator", categoryToLogicalName.get("ToneIndicator"));

        yaml.append("  discovery:\n");
        yaml.append("    enabled: true\n");
        yaml.append("    testOnStartup: true\n");
        yaml.append("    autoAdapt: false\n");

        return yaml.toString();
    }

    // ---- Private helpers ----

    private DeviceInfo entryToDeviceInfo(SimpleEntry entry) {
        String logicalName = safeGetProperty(entry, "logicalName");
        String category = safeGetProperty(entry, "deviceCategory");
        String productName = safeGetProperty(entry, "productName");
        String description = safeGetProperty(entry, "productDescription");
        String vendorName = safeGetProperty(entry, "vendorName");
        String version = safeGetProperty(entry, "jposVersion");
        String factoryClass = safeGetProperty(entry, "serviceInstanceFactoryClass");
        String serviceClass = safeGetProperty(entry, "serviceClass");

        // Collect all properties
        Map<String, String> properties = new LinkedHashMap<>();
        Enumeration<?> propNames = entry.getPropertyNames();
        while (propNames.hasMoreElements()) {
            String propName = propNames.nextElement().toString();
            Object propValue = entry.getPropertyValue(propName);
            properties.put(propName, propValue != null ? propValue.toString() : "");
        }

        return new DeviceInfo(logicalName, category, productName, description,
                vendorName, version, factoryClass, serviceClass, properties);
    }

    private String safeGetProperty(SimpleEntry entry, String propName) {
        try {
            Object value = entry.getPropertyValue(propName);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Creates a JPOS control object for the given device category.
     * Used for testing device connectivity.
     */
    private BaseJposControl createControlForCategory(String category) {
        if (category == null) return null;
        switch (category) {
            case "POSPrinter":
                return new POSPrinter();
            case "CashDrawer":
                return new CashDrawer();
            case "Scanner":
                return new Scanner();
            case "Scale":
                return new Scale();
            case "LineDisplay":
                return new LineDisplay();
            case "MICR":
                return new MICR();
            case "Keylock":
                return new Keylock();
            case "POSKeyboard":
                return new POSKeyboard();
            case "MSR":
                return new MSR();
            case "ToneIndicator":
                return new ToneIndicator();
            default:
                log.failure("Unknown device category for testing: " + category, 1, null);
                return null;
        }
    }

    private void safeClose(BaseJposControl control) {
        try { control.setDeviceEnabled(false); } catch (Exception ignored) {}
        try { control.release(); } catch (Exception ignored) {}
        try { control.close(); } catch (Exception ignored) {}
    }

    private void appendDeviceMapping(StringBuilder yaml, String key, String logicalName) {
        if (logicalName != null && !logicalName.isEmpty()) {
            yaml.append("    ").append(key).append(": \"").append(logicalName).append("\"\n");
        } else {
            yaml.append("    ").append(key).append(": null\n");
        }
    }

    /**
     * For scanner devices that use a deviceType filter, find the first functional
     * device matching that filter from the discovery results.
     */
    private String getCategoryWithFilter(DeviceDiscoveryResponse discovery, String category, String deviceType) {
        if (discovery.getConfiguredDevices() == null || discovery.getTestResults() == null) return null;

        for (int i = 0; i < discovery.getConfiguredDevices().size(); i++) {
            DeviceInfo info = discovery.getConfiguredDevices().get(i);
            if (category.equals(info.getCategory())) {
                Map<String, String> props = info.getProperties();
                if (props != null && deviceType.equals(props.get("deviceType"))) {
                    // Check if it's functional
                    if (i < discovery.getTestResults().size() && discovery.getTestResults().get(i).isFunctional()) {
                        return info.getLogicalName();
                    }
                }
            }
        }
        return null;
    }
}
