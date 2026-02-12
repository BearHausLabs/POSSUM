package com.target.devicemanager.common;

import jpos.BaseJposControl;
import jpos.JposException;
import jpos.config.JposEntryRegistry;
import jpos.config.simple.SimpleEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class DeviceConnector<T extends BaseJposControl> {

    private final T device;
    private final AbstractMap.SimpleEntry<String, String> customFilter;
    private final JposEntryRegistry deviceRegistry;
    private final String preferredLogicalName;
    private final boolean autoAdapt;
    private String connectedDeviceName;
    private static final int CLAIM_TIMEOUT_IN_MSEC = 30000;
    private final int RETRY_REGISTRY_LOAD = 5;
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceConnector.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("Common", "DeviceConnector", LOGGER);


    public DeviceConnector(T device, JposEntryRegistry deviceRegistry) {
        this(device, deviceRegistry, null, null, true);
    }

    public DeviceConnector(T device, JposEntryRegistry deviceRegistry, AbstractMap.SimpleEntry<String, String> customFilter) {
        this(device, deviceRegistry, customFilter, null, true);
    }

    /**
     * Full constructor with explicit logical name from workstation config.
     *
     * @param device           The JPOS control object
     * @param deviceRegistry   The JPOS entry registry (from devcon.xml)
     * @param customFilter     Optional filter for device type (e.g., Flatbed vs HandScanner)
     * @param preferredLogicalName  Explicit logical name from possum-config.yml (null = auto-discover)
     * @param autoAdapt        If true and preferred name fails, fall back to auto-discovery
     */
    public DeviceConnector(T device, JposEntryRegistry deviceRegistry,
                           AbstractMap.SimpleEntry<String, String> customFilter,
                           String preferredLogicalName, boolean autoAdapt) {
        if (device == null) {
            throw new IllegalArgumentException("device cannot be null");
        }
        if (deviceRegistry == null) {
            throw new IllegalArgumentException("deviceRegistry cannot be null");
        }
        this.device = device;
        this.customFilter = customFilter;
        this.deviceRegistry = deviceRegistry;
        this.preferredLogicalName = preferredLogicalName;
        this.autoAdapt = autoAdapt;
        this.connectedDeviceName = getDefaultDeviceName();
    }

    boolean discoverConnectedDevice() {
        // If an explicit logical name is configured, try it first
        if (preferredLogicalName != null && !preferredLogicalName.isEmpty()) {
            log.success("Trying preferred device name: '" + preferredLogicalName + "'", 9);
            clearDeviceCache();
            boolean isConnected = connect(preferredLogicalName);
            if (isConnected) {
                log.success("device found via preferred name '" + connectedDeviceName + "'", 9);
                return true;
            }
            log.failure("preferred device '" + preferredLogicalName + "' not available", 1, null);
            if (!autoAdapt) {
                log.failure("autoAdapt is disabled, skipping auto-discovery", 1, null);
                return false;
            }
            log.success("Falling back to auto-discovery...", 9);
        }

        // Auto-discover: try all matching logical names from the registry
        List<String> configNames = getLogicalNamesForDeviceType();
        for (String configName : configNames) {
            clearDeviceCache();
            boolean isConnected = connect(configName);
            if (isConnected) {
                log.success("device found '" + connectedDeviceName + "'", 9);
                return true;
            }
        }
        return false;
    }

    String getConnectedDeviceName() {
        return this.connectedDeviceName;
    }

    private void clearDeviceCache() {
        synchronized (device) {
            try {
                device.setDeviceEnabled(false);
            } catch (Exception exception) {
                log.failure("failed to disable device '" + getDefaultDeviceName() + "'" + exception, 1, exception);
            }
            try {
                device.release();
            } catch (Exception exception) {
                log.failure("failed to release device '" + getDefaultDeviceName() + "'" + exception, 1, exception);
            }
            try {
                device.close();
            } catch (Exception exception) {
                log.failure("failed to close device '" + getDefaultDeviceName() + "'" + exception, 1, exception);
            }
        }
    }

    private boolean connect(String configName) {
            synchronized (device) {
                try {
                    device.open(configName);
                } catch (JposException jposException){
                    log.failure("failed to open " + configName + " with error " + jposException.getErrorCode(), 17, jposException);
                    return false;
                }
                try {
                    device.claim(CLAIM_TIMEOUT_IN_MSEC);
                } catch (JposException jposException){
                    log.failure("failed to claim " + configName + " with error " + jposException.getErrorCode(), 17, jposException);
                    return false;
                }
                //this is a test, some devices wont signal connected status until enabled
                //then disable to put it back in the same state
                try {
                    device.setDeviceEnabled(true);
                } catch (JposException jposException){
                    log.failure("failed to enable " + configName + " with error " + jposException.getErrorCode(), 17, jposException);
                    return false;
                }
                try {
                    device.setDeviceEnabled(false);
                } catch (JposException jposException){
                    log.failure("failed to disable " + configName + " with error " + jposException.getErrorCode(), 17, jposException);
                    return false;
                }
                this.connectedDeviceName = configName;
                log.success("successfully connected " + configName, 9);
                return true;
            }
    }

    private List<String> getLogicalNamesForDeviceType() {
        for(int i = 0; i < RETRY_REGISTRY_LOAD && deviceRegistry.getSize() == 0; i++) {
            deviceRegistry.load();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException interruptedException) {
                //ignore
            }
        }

        ArrayList<SimpleEntry> list = Collections.list((Enumeration<SimpleEntry>)deviceRegistry.getEntries());
        return list
                .stream()
                .filter(x -> {
                    String deviceCategory = x.getPropertyValue("deviceCategory").toString();
                    Class<? extends BaseJposControl> deviceClass = device.getClass();
                    return deviceCategory.equals(deviceClass.getSimpleName());
                })
                .filter(x -> {
                    if (customFilter == null) return true;

                    String customFilterValue = x.getPropertyValue(customFilter.getKey()).toString();
                    return customFilter.getValue().equals(customFilterValue);
                })
                .map(x -> x.getPropertyValue("logicalName").toString())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String getDefaultDeviceName() {
        if(this.customFilter != null) {
            return customFilter.getValue();
        }
        return device.getClass().getSimpleName();
    }
}
