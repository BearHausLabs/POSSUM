package com.target.devicemanager.components.poskeyboard.entities;

/**
 * Represents a single key press or release event from the POS keyboard hardware.
 * This is the data structure that flows from the device through SSE to connected clients.
 */
public class KeyboardEventData {
    private final int keyCode;
    private final String eventType;
    private final long timestamp;

    public KeyboardEventData(int keyCode, String eventType, long timestamp) {
        this.keyCode = keyCode;
        this.eventType = eventType;
        this.timestamp = timestamp;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public String getEventType() {
        return eventType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "KeyboardEventData{" +
                "keyCode=" + keyCode +
                ", eventType='" + eventType + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
