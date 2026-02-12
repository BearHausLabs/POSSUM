package com.target.devicemanager.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Workstation configuration loaded from possum-config.yml (or application properties).
 *
 * Provides explicit device-to-logical-name mappings so POSSUM can skip
 * auto-discovery and connect directly to known devices. When device mappings
 * are null, the existing auto-discovery in DeviceConnector is used.
 *
 * Override with: --spring.config.additional-location=file:/path/to/possum-config.yml
 */
@Configuration
@ConfigurationProperties(prefix = "possum")
public class WorkstationConfig {

    private StoreInfo store;
    private WorkstationInfo workstation;
    private DeviceMappings devices;
    private DiscoverySettings discovery;

    // ---- Getters and Setters ----

    public StoreInfo getStore() {
        return store;
    }

    public void setStore(StoreInfo store) {
        this.store = store;
    }

    public WorkstationInfo getWorkstation() {
        return workstation;
    }

    public void setWorkstation(WorkstationInfo workstation) {
        this.workstation = workstation;
    }

    public DeviceMappings getDevices() {
        return devices;
    }

    public void setDevices(DeviceMappings devices) {
        this.devices = devices;
    }

    public DiscoverySettings getDiscovery() {
        return discovery;
    }

    public void setDiscovery(DiscoverySettings discovery) {
        this.discovery = discovery;
    }

    // ---- Helper methods ----

    /**
     * Returns the explicit logical name for a device type, or null if not configured.
     */
    public String getDeviceLogicalName(String deviceKey) {
        if (devices == null) return null;
        switch (deviceKey) {
            case "printer": return devices.getPrinter();
            case "cashDrawer": return devices.getCashDrawer();
            case "flatbedScanner": return devices.getFlatbedScanner();
            case "handScanner": return devices.getHandScanner();
            case "scale": return devices.getScale();
            case "lineDisplay": return devices.getLineDisplay();
            case "micr": return devices.getMicr();
            case "keylock": return devices.getKeylock();
            case "posKeyboard": return devices.getPosKeyboard();
            case "msr": return devices.getMsr();
            case "toneIndicator": return devices.getToneIndicator();
            default: return null;
        }
    }

    public boolean isTestOnStartup() {
        return discovery != null && discovery.isTestOnStartup();
    }

    public boolean isAutoAdapt() {
        return discovery != null && discovery.isAutoAdapt();
    }

    public boolean isDiscoveryEnabled() {
        return discovery != null && discovery.isEnabled();
    }

    // ---- Nested POJOs ----

    public static class StoreInfo {
        private String id;
        private String name;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class WorkstationInfo {
        private String id;
        private String name;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class DeviceMappings {
        private String printer;
        private String cashDrawer;
        private String flatbedScanner;
        private String handScanner;
        private String scale;
        private String lineDisplay;
        private String micr;
        private String keylock;
        private String posKeyboard;
        private String msr;
        private String toneIndicator;

        public String getPrinter() { return printer; }
        public void setPrinter(String printer) { this.printer = printer; }
        public String getCashDrawer() { return cashDrawer; }
        public void setCashDrawer(String cashDrawer) { this.cashDrawer = cashDrawer; }
        public String getFlatbedScanner() { return flatbedScanner; }
        public void setFlatbedScanner(String flatbedScanner) { this.flatbedScanner = flatbedScanner; }
        public String getHandScanner() { return handScanner; }
        public void setHandScanner(String handScanner) { this.handScanner = handScanner; }
        public String getScale() { return scale; }
        public void setScale(String scale) { this.scale = scale; }
        public String getLineDisplay() { return lineDisplay; }
        public void setLineDisplay(String lineDisplay) { this.lineDisplay = lineDisplay; }
        public String getMicr() { return micr; }
        public void setMicr(String micr) { this.micr = micr; }
        public String getKeylock() { return keylock; }
        public void setKeylock(String keylock) { this.keylock = keylock; }
        public String getPosKeyboard() { return posKeyboard; }
        public void setPosKeyboard(String posKeyboard) { this.posKeyboard = posKeyboard; }
        public String getMsr() { return msr; }
        public void setMsr(String msr) { this.msr = msr; }
        public String getToneIndicator() { return toneIndicator; }
        public void setToneIndicator(String toneIndicator) { this.toneIndicator = toneIndicator; }
    }

    public static class DiscoverySettings {
        private boolean enabled = true;
        private boolean testOnStartup = false;
        private boolean autoAdapt = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isTestOnStartup() { return testOnStartup; }
        public void setTestOnStartup(boolean testOnStartup) { this.testOnStartup = testOnStartup; }
        public boolean isAutoAdapt() { return autoAdapt; }
        public void setAutoAdapt(boolean autoAdapt) { this.autoAdapt = autoAdapt; }
    }
}
