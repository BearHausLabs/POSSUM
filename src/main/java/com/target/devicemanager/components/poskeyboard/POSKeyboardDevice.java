package com.target.devicemanager.components.poskeyboard;

import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.components.poskeyboard.entities.POSKeyData;
import jpos.JposConst;
import jpos.JposException;
import jpos.POSKeyboard;
import jpos.POSKeyboardConst;
import jpos.events.DataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * POSKeyboard device implementation following POSSUM patterns.
 * Integrates JavaPOS POSKeyboard with Spring Boot REST APIs.
 */
public class POSKeyboardDevice {
    
    private final DynamicDevice<? extends POSKeyboard> dynamicKeyboard;
    private final DeviceListener deviceListener;
    private boolean deviceConnected = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(POSKeyboardDevice.class);
    private static final Marker MARKER = MarkerFactory.getMarker("FATAL");
    private final ReentrantLock connectLock;
    private boolean isLocked = false;
    
    // BlockingQueue for async key event handling
    private final BlockingQueue<POSKeyData> keyDataQueue;
    
    /**
     * Initializes POSKeyboard device.
     * 
     * @param deviceListener the device listener for events
     * @param dynamicKeyboard the dynamic device wrapper
     */
    public POSKeyboardDevice(DeviceListener deviceListener, 
                             DynamicDevice<? extends POSKeyboard> dynamicKeyboard) {
        this(deviceListener, dynamicKeyboard, new ReentrantLock(true));
    }
    
    public POSKeyboardDevice(DeviceListener deviceListener,
                             DynamicDevice<? extends POSKeyboard> dynamicKeyboard,
                             ReentrantLock connectLock) {
        if (deviceListener == null) {
            LOGGER.error(MARKER, "POSKeyboard Failed in Constructor: deviceListener cannot be null");
            throw new IllegalArgumentException("deviceListener cannot be null");
        }
        if (dynamicKeyboard == null) {
            LOGGER.error(MARKER, "POSKeyboard Failed in Constructor: dynamicKeyboard cannot be null");
            throw new IllegalArgumentException("dynamicKeyboard cannot be null");
        }
        this.dynamicKeyboard = dynamicKeyboard;
        this.deviceListener = deviceListener;
        this.connectLock = connectLock;
        this.keyDataQueue = new LinkedBlockingQueue<>();
    }
    
    /**
     * Makes sure a connection occurs.
     */
    public void connect() {
        if (tryLock()) {
            try {
                DynamicDevice.ConnectionResult connectionResult = dynamicKeyboard.connect();
                if (connectionResult == DynamicDevice.ConnectionResult.CONNECTED) {
                    // CRITICAL: Sleep after open for device initialization (Toshiba requirement)
                    try {
                        Thread.sleep(1000);
                        LOGGER.debug("Waited 1 second for POSKeyboard device initialization");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    attachEventListeners();
                    enable();
                    deviceConnected = true;
                } else if (connectionResult == DynamicDevice.ConnectionResult.NOT_CONNECTED) {
                    deviceConnected = false;
                } else {
                    deviceConnected = (connectionResult == DynamicDevice.ConnectionResult.ALREADY_CONNECTED);
                }
            } catch (JposException e) {
                LOGGER.error("Failed to enable POSKeyboard after connection", e);
                deviceConnected = false;
            } finally {
                unlock();
            }
        }
    }
    
    public Boolean getDeviceConnected() {
        return this.deviceConnected;
    }
    
    public void setDeviceConnected(boolean deviceConnected) {
        this.deviceConnected = deviceConnected;
    }
    
    public Boolean reconnect() throws DeviceException {
        if (tryLock()) {
            try {
                if (deviceConnected) {
                    dynamicKeyboard.disconnect();
                    detachEventListeners();
                }
                
                DynamicDevice.ConnectionResult connectionResult = dynamicKeyboard.connect();
                if (connectionResult == DynamicDevice.ConnectionResult.CONNECTED) {
                    // CRITICAL: Sleep after open
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    attachEventListeners();
                    enable();
                }
                deviceConnected = (connectionResult == DynamicDevice.ConnectionResult.CONNECTED || 
                                 connectionResult == DynamicDevice.ConnectionResult.ALREADY_CONNECTED);
            } catch (JposException e) {
                LOGGER.error("Failed to enable POSKeyboard after reconnection", e);
                deviceConnected = false;
            } finally {
                unlock();
            }
        } else {
            throw new DeviceException(DeviceError.DEVICE_BUSY);
        }
        return deviceConnected;
    }
    
    /**
     * Gets POSKeyboard data with optional timeout.
     * 
     * @param timeoutMs Timeout in milliseconds (0 = wait forever)
     * @return POSKeyData or null if timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public POSKeyData getPOSKeyData(long timeoutMs) throws InterruptedException {
        if (timeoutMs > 0) {
            return keyDataQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        }
        return keyDataQueue.take();
    }
    
    /**
     * Clears any pending key data from the queue.
     */
    public void clearKeyData() {
        keyDataQueue.clear();
        LOGGER.debug("POSKeyboard data queue cleared");
    }
    
    /**
     * Gets the device name.
     * 
     * @return device name
     */
    public String getDeviceName() {
        return dynamicKeyboard.getDeviceName();
    }
    
    /**
     * Makes sure POSKeyboard is connected.
     * 
     * @return Connection status
     */
    public boolean isConnected() {
        return dynamicKeyboard.isConnected();
    }
    
    /**
     * Makes sure POSKeyboard is connected and enabled.
     * 
     * @throws JposException if enabling fails
     */
    protected void enable() throws JposException {
        LOGGER.trace("POSKeyboard enable(in)");
        if (!isConnected()) {
            JposException jposException = new JposException(JposConst.JPOS_E_OFFLINE);
            throw jposException;
        }
        
        deviceListener.startEventListeners();
        try {
            POSKeyboard keyboard;
            synchronized (keyboard = dynamicKeyboard.getDevice()) {
                keyboard.setDeviceEnabled(true);
                // Set event types to receive both key down and key up events
                keyboard.setEventTypes(POSKeyboardConst.KBD_ET_DOWN_UP);
                keyboard.setDataEventEnabled(true);
            }
        } catch (JposException jposException) {
            if (isConnected()) {
                LOGGER.error(MARKER, "POSKeyboard Failed to Enable Device: {} ({})", 
                           jposException.getErrorCode(), jposException.getErrorCodeExtended());
            } else {
                LOGGER.error("POSKeyboard Failed to Enable Device: {} ({})", 
                           jposException.getErrorCode(), jposException.getErrorCodeExtended());
            }
            throw jposException;
        }
        LOGGER.info("POSKeyboard enabled");
        LOGGER.trace("POSKeyboard enable(out)");
    }
    
    /**
     * Attaches event listeners.
     */
    private void attachEventListeners() {
        POSKeyboard keyboard;
        synchronized (keyboard = dynamicKeyboard.getDevice()) {
            keyboard.addErrorListener(deviceListener);
            keyboard.addDataListener(deviceListener);
            keyboard.addStatusUpdateListener(deviceListener);
        }
    }
    
    /**
     * Removes error, data, and status update device listeners.
     */
    private void detachEventListeners() {
        POSKeyboard keyboard;
        synchronized (keyboard = dynamicKeyboard.getDevice()) {
            keyboard.removeErrorListener(deviceListener);
            keyboard.removeDataListener(deviceListener);
            keyboard.removeStatusUpdateListener(deviceListener);
        }
    }
    
    /**
     * Lock the current resource.
     * 
     * @return true if lock acquired
     */
    public boolean tryLock() {
        try {
            isLocked = connectLock.tryLock(10, TimeUnit.SECONDS);
            LOGGER.trace("POSKeyboard Lock: " + isLocked);
        } catch (InterruptedException interruptedException) {
            LOGGER.error("POSKeyboard Lock Failed: " + interruptedException.getMessage());
        }
        return isLocked;
    }
    
    /**
     * Unlock the current resource.
     */
    public void unlock() {
        connectLock.unlock();
        isLocked = false;
    }
    
    /**
     * Process keyboard data event (called by DeviceListener).
     * 
     * @param dataEvent the data event from JavaPOS
     */
    public void handleDataEvent(DataEvent dataEvent) {
        try {
            POSKeyboard keyboard;
            synchronized (keyboard = dynamicKeyboard.getDevice()) {
                int scanCode = keyboard.getPOSKeyData();
                int eventType = keyboard.getPOSKeyEventType();
                
                // Determine event type
                POSKeyData.KeyEventType keyEventType;
                if (eventType == POSKeyboardConst.KBD_KET_KEYDOWN) {
                    keyEventType = POSKeyData.KeyEventType.KEY_DOWN;
                } else if (eventType == POSKeyboardConst.KBD_KET_KEYUP) {
                    keyEventType = POSKeyData.KeyEventType.KEY_UP;
                } else {
                    keyEventType = POSKeyData.KeyEventType.KEY_PRESS;
                }
                
                // Check if extended key (check bit flag)
                boolean isExtended = false; // Simplified for now
                
                // Create POSKeyData object
                POSKeyData keyData = new POSKeyData();
                keyData.setKeyCode(scanCode);
                keyData.setKeyName(getKeyName(scanCode));
                keyData.setExtendedKey(isExtended);
                keyData.setEventType(keyEventType);
                
                // Add to queue (only process key down to avoid duplicates)
                if (eventType == POSKeyboardConst.KBD_KET_KEYDOWN) {
                    keyDataQueue.offer(keyData);
                    LOGGER.debug("POSKeyboard event - Type: {}, ScanCode: 0x{} ({}), Name: {}", 
                               keyEventType, Integer.toHexString(scanCode), scanCode, keyData.getKeyName());
                }
                
                // CRITICAL: Re-enable data events after processing
                keyboard.setDataEventEnabled(true);
            }
        } catch (JposException e) {
            LOGGER.error("Error processing POSKeyboard data event", e);
        }
    }
    
    /**
     * Maps key codes to human-readable names.
     * 
     * @param keyCode the key code
     * @return human-readable key name
     */
    private String getKeyName(int keyCode) {
        int baseCode = keyCode; // Use code directly
        
        // Printable ASCII characters
        if (baseCode >= 32 && baseCode <= 126) {
            return String.valueOf((char) baseCode);
        }
        
        // Special keys
        switch (baseCode) {
            case 8: return "BACKSPACE";
            case 9: return "TAB";
            case 13: return "ENTER";
            case 27: return "ESC";
            case 32: return "SPACE";
            default: return "KEY_" + baseCode;
        }
    }
}

