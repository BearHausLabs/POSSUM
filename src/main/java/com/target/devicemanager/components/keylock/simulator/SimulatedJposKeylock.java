package com.target.devicemanager.components.keylock.simulator;

import jpos.*;
import jpos.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulated JavaPOS Keylock implementation for testing without hardware.
 * Allows simulation of key lock position changes for development and testing.
 */
public class SimulatedJposKeylock extends Keylock {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedJposKeylock.class);
    
    private boolean claimed = false;
    private boolean deviceEnabled = false;
    private int state = JposConst.JPOS_S_CLOSED;
    
    // Current key position
    private int keyPosition = KeylockConst.LOCK_KP_NORM; // Default to NORMAL
    
    private final List<StatusUpdateListener> statusUpdateListeners = new ArrayList<>();
    
    public SimulatedJposKeylock() {
        LOGGER.info("SimulatedJposKeylock created");
    }
    
    @Override
    public void open(String logicalName) throws JposException {
        LOGGER.info("Simulated Keylock open: {}", logicalName);
        state = JposConst.JPOS_S_IDLE;
    }
    
    @Override
    public void claim(int timeout) throws JposException {
        LOGGER.info("Simulated Keylock claim: timeout={}", timeout);
        if (state == JposConst.JPOS_S_CLOSED) {
            throw new JposException(JposConst.JPOS_E_CLOSED, "Device not open");
        }
        claimed = true;
    }
    
    @Override
    public void release() throws JposException {
        LOGGER.info("Simulated Keylock release");
        claimed = false;
    }
    
    @Override
    public void close() throws JposException {
        LOGGER.info("Simulated Keylock close");
        deviceEnabled = false;
        claimed = false;
        state = JposConst.JPOS_S_CLOSED;
        statusUpdateListeners.clear();
    }
    
    @Override
    public boolean getClaimed() throws JposException {
        return claimed;
    }
    
    @Override
    public void setDeviceEnabled(boolean enabled) throws JposException {
        LOGGER.info("Simulated Keylock setDeviceEnabled: {}", enabled);
        if (!claimed) {
            throw new JposException(JposConst.JPOS_E_NOTCLAIMED, "Device not claimed");
        }
        deviceEnabled = enabled;
    }
    
    @Override
    public boolean getDeviceEnabled() throws JposException {
        return deviceEnabled;
    }
    
    @Override
    public int getKeyPosition() throws JposException {
        LOGGER.debug("Simulated Keylock getKeyPosition: {}", keyPosition);
        return keyPosition;
    }
    
    @Override
    public void waitForKeylockChange(int position, int timeout) throws JposException {
        LOGGER.debug("Simulated Keylock waitForKeylockChange: position={}, timeout={}", position, timeout);
        
        if (!deviceEnabled) {
            throw new JposException(JposConst.JPOS_E_DISABLED, "Device not enabled");
        }
        
        // In simulation, we just simulate a timeout if not waiting for current position
        if (position != KeylockConst.LOCK_KP_ANY && position != keyPosition) {
            try {
                Thread.sleep(Math.min(timeout, 1000)); // Simulate waiting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Throw timeout exception
            throw new JposException(JposConst.JPOS_E_TIMEOUT, "Wait timeout");
        }
    }
    
    @Override
    public int getState() {
        return state;
    }
    
    @Override
    public String getPhysicalDeviceName() throws JposException {
        return "SimulatedKeylock";
    }
    
    @Override
    public String getPhysicalDeviceDescription() throws JposException {
        return "Simulated JavaPOS Keylock for Testing";
    }
    
    @Override
    public int getDeviceServiceVersion() throws JposException {
        return 1014000; // Version 1.14
    }
    
    @Override
    public int getPowerState() throws JposException {
        return JposConst.JPOS_PS_ONLINE;
    }
    
    @Override
    public void addStatusUpdateListener(StatusUpdateListener listener) {
        statusUpdateListeners.add(listener);
        LOGGER.debug("Status update listener added, total: {}", statusUpdateListeners.size());
    }
    
    @Override
    public void removeStatusUpdateListener(StatusUpdateListener listener) {
        statusUpdateListeners.remove(listener);
        LOGGER.debug("Status update listener removed, total: {}", statusUpdateListeners.size());
    }
    
    /**
     * Simulates changing the key lock position.
     * Fires status update events to all registered listeners.
     * 
     * @param newPosition The new position (use KeylockConst values)
     */
    public void simulatePositionChange(int newPosition) {
        if (!deviceEnabled) {
            LOGGER.warn("Cannot simulate position change - device not enabled");
            return;
        }
        
        String positionName;
        switch (newPosition) {
            case KeylockConst.LOCK_KP_LOCK:
                positionName = "LOCKED";
                break;
            case KeylockConst.LOCK_KP_NORM:
                positionName = "NORMAL";
                break;
            case KeylockConst.LOCK_KP_SUPR:
                positionName = "SUPERVISOR";
                break;
            default:
                positionName = "UNKNOWN";
        }
        
        LOGGER.info("Simulating key lock position change: {} -> {} ({})", 
                   keyPosition, newPosition, positionName);
        
        this.keyPosition = newPosition;
        
        // Fire status update event to all listeners
        StatusUpdateEvent event = new StatusUpdateEvent(this, newPosition);
        for (StatusUpdateListener listener : new ArrayList<>(statusUpdateListeners)) {
            try {
                listener.statusUpdateOccurred(event);
            } catch (Exception e) {
                LOGGER.error("Error in status update listener", e);
            }
        }
    }
}

