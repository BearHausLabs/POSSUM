package com.target.devicemanager.components.poskeyboard.entities;

/**
 * Data transfer object for POSKeyboard key events.
 * Represents a single key press event from the POS keyboard device.
 */
public class POSKeyData {
    
    private int keyCode;
    private String keyName;
    private boolean isExtendedKey;
    private long timestamp;
    private KeyEventType eventType;
    
    public enum KeyEventType {
        KEY_DOWN,
        KEY_UP,
        KEY_PRESS
    }
    
    public POSKeyData() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public POSKeyData(int keyCode, String keyName, boolean isExtendedKey, KeyEventType eventType) {
        this.keyCode = keyCode;
        this.keyName = keyName;
        this.isExtendedKey = isExtendedKey;
        this.timestamp = System.currentTimeMillis();
        this.eventType = eventType;
    }
    
    public int getKeyCode() {
        return keyCode;
    }
    
    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }
    
    public String getKeyName() {
        return keyName;
    }
    
    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }
    
    public boolean isExtendedKey() {
        return isExtendedKey;
    }
    
    public void setExtendedKey(boolean extendedKey) {
        isExtendedKey = extendedKey;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public KeyEventType getEventType() {
        return eventType;
    }
    
    public void setEventType(KeyEventType eventType) {
        this.eventType = eventType;
    }
    
    @Override
    public String toString() {
        return "POSKeyData{" +
                "keyCode=" + keyCode +
                ", keyName='" + keyName + '\'' +
                ", isExtendedKey=" + isExtendedKey +
                ", timestamp=" + timestamp +
                ", eventType=" + eventType +
                '}';
    }
}

