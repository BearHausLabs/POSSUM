package com.target.devicemanager.common.entities;

import java.util.Map;

/**
 * Represents a single device entry parsed from the JavaPOS configuration (devcon.xml).
 * Contains all metadata about a configured device, including its logical name,
 * category, vendor info, and driver class references.
 */
public class DeviceInfo {

    private final String logicalName;
    private final String category;
    private final String productName;
    private final String description;
    private final String vendorName;
    private final String version;
    private final String factoryClass;
    private final String serviceClass;
    private final Map<String, String> properties;

    public DeviceInfo(String logicalName, String category, String productName,
                      String description, String vendorName, String version,
                      String factoryClass, String serviceClass,
                      Map<String, String> properties) {
        this.logicalName = logicalName != null ? logicalName : "";
        this.category = category != null ? category : "";
        this.productName = productName != null ? productName : "";
        this.description = description != null ? description : "";
        this.vendorName = vendorName != null ? vendorName : "";
        this.version = version != null ? version : "";
        this.factoryClass = factoryClass != null ? factoryClass : "";
        this.serviceClass = serviceClass != null ? serviceClass : "";
        this.properties = properties;
    }

    public String getLogicalName() {
        return logicalName;
    }

    public String getCategory() {
        return category;
    }

    public String getProductName() {
        return productName;
    }

    public String getDescription() {
        return description;
    }

    public String getVendorName() {
        return vendorName;
    }

    public String getVersion() {
        return version;
    }

    public String getFactoryClass() {
        return factoryClass;
    }

    public String getServiceClass() {
        return serviceClass;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "logicalName='" + logicalName + '\'' +
                ", category='" + category + '\'' +
                ", productName='" + productName + '\'' +
                ", vendorName='" + vendorName + '\'' +
                '}';
    }
}
