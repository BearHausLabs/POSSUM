package com.target.devicemanager.common.entities;

import java.time.Instant;
import java.util.List;

/**
 * Full response from a device discovery operation.
 * Contains all configured devices from devcon.xml, optional test results,
 * and metadata about when and where the discovery was performed.
 */
public class DeviceDiscoveryResponse {

    private final List<DeviceInfo> configuredDevices;
    private final List<DeviceTestResult> testResults;
    private final String configFile;
    private final Instant discoveredAt;

    public DeviceDiscoveryResponse(List<DeviceInfo> configuredDevices,
                                   List<DeviceTestResult> testResults,
                                   String configFile,
                                   Instant discoveredAt) {
        this.configuredDevices = configuredDevices;
        this.testResults = testResults;
        this.configFile = configFile;
        this.discoveredAt = discoveredAt;
    }

    public List<DeviceInfo> getConfiguredDevices() {
        return configuredDevices;
    }

    public List<DeviceTestResult> getTestResults() {
        return testResults;
    }

    public String getConfigFile() {
        return configFile;
    }

    public Instant getDiscoveredAt() {
        return discoveredAt;
    }

    @Override
    public String toString() {
        return "DeviceDiscoveryResponse{" +
                "configuredDevices=" + (configuredDevices != null ? configuredDevices.size() : 0) +
                ", testResults=" + (testResults != null ? testResults.size() : 0) +
                ", configFile='" + configFile + '\'' +
                ", discoveredAt=" + discoveredAt +
                '}';
    }
}
