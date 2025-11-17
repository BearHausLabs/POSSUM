package com.target.devicemanager.components.keylock.entities;

/**
 * Data transfer object for KeyLock position status.
 * Represents the current position of the physical key lock.
 */
public class KeyLockStatus {
    
    private int keyPosition;
    private String positionName;
    private long timestamp;
    
    public enum KeyPosition {
        LOCK_KP_ANY(0, "ANY"),
        LOCK_KP_LOCK(1, "LOCKED"),
        LOCK_KP_NORM(2, "NORMAL"),
        LOCK_KP_SUPR(3, "SUPERVISOR");
        
        private final int value;
        private final String name;
        
        KeyPosition(int value, String name) {
            this.value = value;
            this.name = name;
        }
        
        public int getValue() {
            return value;
        }
        
        public String getName() {
            return name;
        }
        
        public static KeyPosition fromValue(int value) {
            for (KeyPosition pos : values()) {
                if (pos.value == value) {
                    return pos;
                }
            }
            return LOCK_KP_ANY;
        }
    }
    
    public KeyLockStatus() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public KeyLockStatus(int keyPosition, String positionName) {
        this.keyPosition = keyPosition;
        this.positionName = positionName;
        this.timestamp = System.currentTimeMillis();
    }
    
    public int getKeyPosition() {
        return keyPosition;
    }
    
    public void setKeyPosition(int keyPosition) {
        this.keyPosition = keyPosition;
        KeyPosition pos = KeyPosition.fromValue(keyPosition);
        this.positionName = pos.getName();
    }
    
    public String getPositionName() {
        return positionName;
    }
    
    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "KeyLockStatus{" +
                "keyPosition=" + keyPosition +
                ", positionName='" + positionName + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

