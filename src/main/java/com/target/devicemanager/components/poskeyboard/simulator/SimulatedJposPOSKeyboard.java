package com.target.devicemanager.components.poskeyboard.simulator;

import jpos.*;
import jpos.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulated JavaPOS POSKeyboard implementation for testing without hardware.
 * Allows injection of simulated key events for development and testing.
 */
public class SimulatedJposPOSKeyboard extends POSKeyboard {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedJposPOSKeyboard.class);
    
    private boolean claimed = false;
    private boolean deviceEnabled = false;
    private boolean dataEventEnabled = false;
    private int state = JposConst.JPOS_S_CLOSED;
    
    // Simulated data
    private int lastPOSKeyData = 0;
    private int lastPOSKeyEventType = POSKeyboardConst.KBD_KET_KEYDOWN;
    
    private final List<DataListener> dataListeners = new ArrayList<>();
    private final List<ErrorListener> errorListeners = new ArrayList<>();
    private final List<StatusUpdateListener> statusUpdateListeners = new ArrayList<>();
    
    public SimulatedJposPOSKeyboard() {
        LOGGER.info("SimulatedJposPOSKeyboard created");
    }
    
    @Override
    public void open(String logicalName) throws JposException {
        LOGGER.info("Simulated POSKeyboard open: {}", logicalName);
        state = JposConst.JPOS_S_IDLE;
    }
    
    @Override
    public void claim(int timeout) throws JposException {
        LOGGER.info("Simulated POSKeyboard claim: timeout={}", timeout);
        if (state == JposConst.JPOS_S_CLOSED) {
            throw new JposException(JposConst.JPOS_E_CLOSED, "Device not open");
        }
        claimed = true;
    }
    
    @Override
    public void release() throws JposException {
        LOGGER.info("Simulated POSKeyboard release");
        claimed = false;
    }
    
    @Override
    public void close() throws JposException {
        LOGGER.info("Simulated POSKeyboard close");
        deviceEnabled = false;
        claimed = false;
        state = JposConst.JPOS_S_CLOSED;
        dataListeners.clear();
        errorListeners.clear();
        statusUpdateListeners.clear();
    }
    
    @Override
    public boolean getClaimed() throws JposException {
        return claimed;
    }
    
    @Override
    public void setDeviceEnabled(boolean enabled) throws JposException {
        LOGGER.info("Simulated POSKeyboard setDeviceEnabled: {}", enabled);
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
    public void setEventTypes(int eventTypes) throws JposException {
        LOGGER.info("Simulated POSKeyboard setEventTypes: {}", eventTypes);
        // Event types are logged but not stored in simulator
    }
    
    @Override
    public void setDataEventEnabled(boolean enabled) throws JposException {
        LOGGER.debug("Simulated POSKeyboard setDataEventEnabled: {}", enabled);
        dataEventEnabled = enabled;
    }
    
    @Override
    public boolean getDataEventEnabled() throws JposException {
        return dataEventEnabled;
    }
    
    @Override
    public int getPOSKeyData() throws JposException {
        return lastPOSKeyData;
    }
    
    @Override
    public int getPOSKeyEventType() throws JposException {
        return lastPOSKeyEventType;
    }
    
    @Override
    public int getState() {
        return state;
    }
    
    @Override
    public String getPhysicalDeviceName() throws JposException {
        return "SimulatedPOSKeyboard";
    }
    
    @Override
    public String getPhysicalDeviceDescription() throws JposException {
        return "Simulated JavaPOS POSKeyboard for Testing";
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
    public void addDataListener(DataListener listener) {
        dataListeners.add(listener);
        LOGGER.debug("Data listener added, total: {}", dataListeners.size());
    }
    
    @Override
    public void removeDataListener(DataListener listener) {
        dataListeners.remove(listener);
        LOGGER.debug("Data listener removed, total: {}", dataListeners.size());
    }
    
    @Override
    public void addErrorListener(ErrorListener listener) {
        errorListeners.add(listener);
    }
    
    @Override
    public void removeErrorListener(ErrorListener listener) {
        errorListeners.remove(listener);
    }
    
    @Override
    public void addStatusUpdateListener(StatusUpdateListener listener) {
        statusUpdateListeners.add(listener);
    }
    
    @Override
    public void removeStatusUpdateListener(StatusUpdateListener listener) {
        statusUpdateListeners.remove(listener);
    }
    
    /**
     * Simulates a key press event for testing.
     * 
     * @param keyCode The key code to simulate
     * @param eventType The event type (KEY_DOWN or KEY_UP)
     */
    public void simulateKeyPress(int keyCode, int eventType) {
        if (!deviceEnabled || !dataEventEnabled) {
            LOGGER.warn("Cannot simulate key press - device not enabled or data events disabled");
            return;
        }
        
        this.lastPOSKeyData = keyCode;
        this.lastPOSKeyEventType = eventType;
        
        LOGGER.info("Simulating key press: keyCode=0x{} ({}), eventType={}", 
                   Integer.toHexString(keyCode), keyCode, 
                   eventType == POSKeyboardConst.KBD_KET_KEYDOWN ? "DOWN" : "UP");
        
        // Fire data event to all listeners
        DataEvent dataEvent = new DataEvent(this, 0);
        for (DataListener listener : new ArrayList<>(dataListeners)) {
            try {
                listener.dataOccurred(dataEvent);
            } catch (Exception e) {
                LOGGER.error("Error in data listener", e);
            }
        }
    }
    
    /**
     * Simulates a key press (both down and up events).
     * 
     * @param keyCode The key code to simulate
     */
    public void simulateKeyPress(int keyCode) {
        simulateKeyPress(keyCode, POSKeyboardConst.KBD_KET_KEYDOWN);
        
        // Small delay between down and up
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        simulateKeyPress(keyCode, POSKeyboardConst.KBD_KET_KEYUP);
    }
}

