package com.target.devicemanager.common.entities;

/**
 * Result of testing a single JavaPOS device's connectivity.
 * Records whether the device could be opened, claimed, and enabled,
 * along with timing information and any error messages.
 */
public class DeviceTestResult {

    private final String logicalName;
    private final String category;
    private final boolean functional;
    private final String message;
    private final long testDurationMs;

    public DeviceTestResult(String logicalName, String category, boolean functional,
                            String message, long testDurationMs) {
        this.logicalName = logicalName != null ? logicalName : "";
        this.category = category != null ? category : "";
        this.functional = functional;
        this.message = message != null ? message : "";
        this.testDurationMs = testDurationMs;
    }

    public String getLogicalName() {
        return logicalName;
    }

    public String getCategory() {
        return category;
    }

    public boolean isFunctional() {
        return functional;
    }

    public String getMessage() {
        return message;
    }

    public long getTestDurationMs() {
        return testDurationMs;
    }

    @Override
    public String toString() {
        return "DeviceTestResult{" +
                "logicalName='" + logicalName + '\'' +
                ", category='" + category + '\'' +
                ", functional=" + functional +
                ", message='" + message + '\'' +
                ", testDurationMs=" + testDurationMs +
                '}';
    }
}
