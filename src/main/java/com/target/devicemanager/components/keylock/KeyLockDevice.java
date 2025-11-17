package com.target.devicemanager.components.keylock;

import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.components.keylock.entities.KeyLockStatus;
import jpos.JposConst;
import jpos.JposException;
import jpos.Keylock;
import jpos.KeylockConst;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * KeyLock device implementation following POSSUM patterns.
 * Integrates JavaPOS Keylock with Spring Boot REST APIs.
 * Implements StatusUpdateListener to receive position change events.
 */
public class KeyLockDevice implements StatusUpdateListener {
    
    private final DynamicDevice<? extends Keylock> dynamicKeylock;
    private boolean deviceConnected = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyLockDevice.class);
    private static final Marker MARKER = MarkerFactory.getMarker("FATAL");
    private final ReentrantLock connectLock;
    private boolean isLocked = false;
    
    // Current key lock position
    private final AtomicInteger currentPosition;
    
    /**
     * Initializes KeyLock device.
     * 
     * @param dynamicKeylock the dynamic device wrapper
     */
    public KeyLockDevice(DynamicDevice<? extends Keylock> dynamicKeylock) {
        this(dynamicKeylock, new ReentrantLock(true));
    }
    
    public KeyLockDevice(DynamicDevice<? extends Keylock> dynamicKeylock,
                         ReentrantLock connectLock) {
        if (dynamicKeylock == null) {
            LOGGER.error(MARKER, "KeyLock Failed in Constructor: dynamicKeylock cannot be null");
            throw new IllegalArgumentException("dynamicKeylock cannot be null");
        }
        this.dynamicKeylock = dynamicKeylock;
        this.connectLock = connectLock;
        this.currentPosition = new AtomicInteger(KeylockConst.LOCK_KP_ANY);
    }
    
    /**
     * Makes sure a connection occurs.
     */
    public void connect() {
        if (tryLock()) {
            try {
                DynamicDevice.ConnectionResult connectionResult = dynamicKeylock.connect();
                if (connectionResult == DynamicDevice.ConnectionResult.CONNECTED) {
                    // CRITICAL: Sleep after open for device initialization
                    try {
                        Thread.sleep(1000);
                        LOGGER.debug("Waited 1 second for KeyLock device initialization");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    attachEventListeners();
                    enable();
                    deviceConnected = true;
                    
                    // Read initial position
                    try {
                        updateCurrentPosition();
                    } catch (JposException e) {
                        LOGGER.warn("Failed to read initial KeyLock position", e);
                    }
                } else if (connectionResult == DynamicDevice.ConnectionResult.NOT_CONNECTED) {
                    deviceConnected = false;
                } else {
                    deviceConnected = (connectionResult == DynamicDevice.ConnectionResult.ALREADY_CONNECTED);
                }
            } catch (JposException e) {
                LOGGER.error("Failed to enable KeyLock after connection", e);
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
                    dynamicKeylock.disconnect();
                    detachEventListeners();
                }
                
                DynamicDevice.ConnectionResult connectionResult = dynamicKeylock.connect();
                if (connectionResult == DynamicDevice.ConnectionResult.CONNECTED) {
                    // CRITICAL: Sleep after open
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    attachEventListeners();
                    enable();
                    
                    // Read initial position
                    try {
                        updateCurrentPosition();
                    } catch (JposException e) {
                        LOGGER.warn("Failed to read KeyLock position after reconnect", e);
                    }
                }
                deviceConnected = (connectionResult == DynamicDevice.ConnectionResult.CONNECTED || 
                                 connectionResult == DynamicDevice.ConnectionResult.ALREADY_CONNECTED);
            } catch (JposException e) {
                LOGGER.error("Failed to enable KeyLock after reconnection", e);
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
     * Gets the current key lock position.
     * 
     * @return KeyLockStatus with current position
     * @throws JposException if reading position fails
     */
    public KeyLockStatus getKeyPosition() throws JposException {
        LOGGER.trace("KeyLock getKeyPosition(in)");
        if (!isConnected()) {
            throw new JposException(JposConst.JPOS_E_OFFLINE);
        }
        
        try {
            Keylock keylock;
            int position;
            synchronized (keylock = dynamicKeylock.getDevice()) {
                position = keylock.getKeyPosition();
            }
            
            currentPosition.set(position);
            
            KeyLockStatus status = new KeyLockStatus();
            status.setKeyPosition(position);
            
            LOGGER.debug("KeyLock position: {} ({})", position, status.getPositionName());
            LOGGER.trace("KeyLock getKeyPosition(out)");
            
            return status;
        } catch (JposException jposException) {
            LOGGER.error("KeyLock Failed to Get Position: {} ({})", 
                       jposException.getErrorCode(), jposException.getErrorCodeExtended());
            throw jposException;
        }
    }
    
    /**
     * Waits for keylock to change to specified position.
     * 
     * @param position Target position (use KeylockConst.LOCK_KP_ANY for any change)
     * @param timeout Timeout in milliseconds
     * @throws JposException if wait fails
     */
    public void waitForKeylockChange(int position, int timeout) throws JposException {
        LOGGER.trace("KeyLock waitForKeylockChange(in)");
        if (!isConnected()) {
            throw new JposException(JposConst.JPOS_E_OFFLINE);
        }
        
        LOGGER.debug("Waiting for keylock position: {} with timeout: {}ms", position, timeout);
        
        try {
            Keylock keylock;
            synchronized (keylock = dynamicKeylock.getDevice()) {
                keylock.waitForKeylockChange(position, timeout);
            }
        } catch (JposException jposException) {
            // Timeout is expected and not an error
            if (jposException.getErrorCode() != JposConst.JPOS_E_TIMEOUT) {
                LOGGER.error("KeyLock Failed to Wait for Change: {} ({})", 
                           jposException.getErrorCode(), jposException.getErrorCodeExtended());
                throw jposException;
            }
        }
        LOGGER.trace("KeyLock waitForKeylockChange(out)");
    }
    
    /**
     * Updates the current position from the device.
     */
    private void updateCurrentPosition() throws JposException {
        Keylock keylock;
        synchronized (keylock = dynamicKeylock.getDevice()) {
            int position = keylock.getKeyPosition();
            currentPosition.set(position);
        }
    }
    
    /**
     * Gets the device name.
     * 
     * @return device name
     */
    public String getDeviceName() {
        return dynamicKeylock.getDeviceName();
    }
    
    /**
     * Makes sure KeyLock is connected.
     * 
     * @return Connection status
     */
    public boolean isConnected() {
        return dynamicKeylock.isConnected();
    }
    
    /**
     * Makes sure KeyLock is connected and enabled.
     * 
     * @throws JposException if enabling fails
     */
    protected void enable() throws JposException {
        LOGGER.trace("KeyLock enable(in)");
        if (!isConnected()) {
            throw new JposException(JposConst.JPOS_E_OFFLINE);
        }
        
        try {
            Keylock keylock;
            synchronized (keylock = dynamicKeylock.getDevice()) {
                keylock.setDeviceEnabled(true);
            }
        } catch (JposException jposException) {
            if (isConnected()) {
                LOGGER.error(MARKER, "KeyLock Failed to Enable Device: {} ({})", 
                           jposException.getErrorCode(), jposException.getErrorCodeExtended());
            } else {
                LOGGER.error("KeyLock Failed to Enable Device: {} ({})", 
                           jposException.getErrorCode(), jposException.getErrorCodeExtended());
            }
            throw jposException;
        }
        LOGGER.info("KeyLock enabled");
        LOGGER.trace("KeyLock enable(out)");
    }
    
    /**
     * Attaches event listeners.
     */
    private void attachEventListeners() {
        Keylock keylock;
        synchronized (keylock = dynamicKeylock.getDevice()) {
            keylock.addStatusUpdateListener(this);
        }
    }
    
    /**
     * Removes event listeners.
     */
    private void detachEventListeners() {
        Keylock keylock;
        synchronized (keylock = dynamicKeylock.getDevice()) {
            keylock.removeStatusUpdateListener(this);
        }
    }
    
    /**
     * StatusUpdateListener implementation.
     * Called when KeyLock position changes.
     */
    @Override
    public void statusUpdateOccurred(StatusUpdateEvent event) {
        int newPosition = event.getStatus();
        currentPosition.set(newPosition);
        
        KeyLockStatus.KeyPosition pos = KeyLockStatus.KeyPosition.fromValue(newPosition);
        LOGGER.info("KeyLock position changed to: {} ({})", newPosition, pos.getName());
    }
    
    /**
     * Lock the current resource.
     * 
     * @return true if lock acquired
     */
    public boolean tryLock() {
        try {
            isLocked = connectLock.tryLock(10, TimeUnit.SECONDS);
            LOGGER.trace("KeyLock Lock: " + isLocked);
        } catch (InterruptedException interruptedException) {
            LOGGER.error("KeyLock Lock Failed: " + interruptedException.getMessage());
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
}

