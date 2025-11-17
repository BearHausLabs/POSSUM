package com.target.devicemanager.components.keylock;

import com.target.devicemanager.common.entities.*;
import com.target.devicemanager.components.keylock.entities.KeyLockStatus;
import jpos.JposException;
import jpos.KeylockConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manager for KeyLock device operations.
 * Handles device lifecycle, health monitoring, and position reading operations.
 */
@EnableScheduling
@EnableCaching
public class KeyLockManager {
    
    @Autowired
    private CacheManager cacheManager;
    
    private final KeyLockDevice keyLockDevice;
    private ConnectEnum connectStatus = ConnectEnum.FIRST_CONNECT;
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyLockManager.class);
    
    public KeyLockManager(KeyLockDevice keyLockDevice) {
        this(keyLockDevice, null);
    }
    
    public KeyLockManager(KeyLockDevice keyLockDevice, CacheManager cacheManager) {
        if (keyLockDevice == null) {
            throw new IllegalArgumentException("keyLockDevice cannot be null");
        }
        this.keyLockDevice = keyLockDevice;
        
        if (cacheManager != null) {
            this.cacheManager = cacheManager;
        }
    }
    
    /**
     * Scheduled connection check - runs every 5 seconds after initial 5 second delay.
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void connect() {
        keyLockDevice.connect();
        
        if (connectStatus == ConnectEnum.FIRST_CONNECT) {
            if (!keyLockDevice.isConnected()) {
                LOGGER.error("KeyLock Failed to Connect");
            }
            connectStatus = ConnectEnum.CHECK_HEALTH;
        }
    }
    
    /**
     * Reconnects the KeyLock device.
     * 
     * @throws DeviceException if reconnection fails
     */
    public void reconnectKeyLock() throws DeviceException {
        try {
            if (!keyLockDevice.reconnect()) {
                LOGGER.error("Failed to reconnect KeyLock.");
                throw new DeviceException(DeviceError.DEVICE_OFFLINE);
            }
        } catch (DeviceException e) {
            throw e;
        }
    }
    
    /**
     * Gets the current key lock position.
     * 
     * @return KeyLockStatus with current position
     * @throws JposException if reading position fails
     */
    public KeyLockStatus getKeyPosition() throws JposException {
        LOGGER.trace("getKeyPosition(in)");
        try {
            return keyLockDevice.getKeyPosition();
        } finally {
            LOGGER.trace("getKeyPosition(out)");
        }
    }
    
    /**
     * Waits for any keylock position change.
     * 
     * @param timeout Timeout in milliseconds
     * @throws JposException if wait fails
     */
    public void waitForAnyChange(int timeout) throws JposException {
        LOGGER.trace("waitForAnyChange(in)");
        try {
            keyLockDevice.waitForKeylockChange(KeylockConst.LOCK_KP_ANY, timeout);
        } finally {
            LOGGER.trace("waitForAnyChange(out)");
        }
    }
    
    /**
     * Waits for keylock to change to specific position.
     * 
     * @param position Target position
     * @param timeout Timeout in milliseconds
     * @throws JposException if wait fails
     */
    public void waitForPosition(int position, int timeout) throws JposException {
        LOGGER.trace("waitForPosition(in)");
        try {
            keyLockDevice.waitForKeylockChange(position, timeout);
        } finally {
            LOGGER.trace("waitForPosition(out)");
        }
    }
    
    /**
     * Gets the health status of the KeyLock.
     * 
     * @return List of device health responses
     */
    public List<DeviceHealthResponse> getHealth() {
        LOGGER.trace("getHealth(in)");
        List<DeviceHealthResponse> response = new ArrayList<>();
        
        if (keyLockDevice.isConnected()) {
            response.add(new DeviceHealthResponse(keyLockDevice.getDeviceName(), DeviceHealth.READY));
        } else {
            response.add(new DeviceHealthResponse(keyLockDevice.getDeviceName(), DeviceHealth.NOTREADY));
        }
        
        try {
            Objects.requireNonNull(cacheManager.getCache("keyLockHealth")).put("health", response);
        } catch (Exception exception) {
            LOGGER.error("getCache(keyLockHealth) Failed: " + exception.getMessage());
        }
        LOGGER.trace("getHealth(out)");
        return response;
    }
    
    /**
     * Gets the cached health status of the KeyLock.
     * 
     * @return List of device health responses
     */
    @SuppressWarnings("unchecked")
    public List<DeviceHealthResponse> getStatus() {
        try {
            if (cacheManager != null && Objects.requireNonNull(cacheManager.getCache("keyLockHealth")).get("health") != null) {
                if (connectStatus == ConnectEnum.CHECK_HEALTH) {
                    connectStatus = ConnectEnum.HEALTH_UPDATED;
                    return getHealth();
                }
                return (List<DeviceHealthResponse>) Objects.requireNonNull(cacheManager.getCache("keyLockHealth")).get("health").get();
            } else {
                LOGGER.debug("Not able to retrieve from cache, checking getHealth()");
                return getHealth();
            }
        } catch (Exception exception) {
            return getHealth();
        }
    }
    
    /**
     * Gets the health status of the KeyLock device.
     * 
     * @return DeviceHealth status
     */
    public DeviceHealth getKeyLockHealthStatus() {
        for (DeviceHealthResponse deviceHealthResponse : getStatus()) {
            if (deviceHealthResponse.getDeviceName().equals(keyLockDevice.getDeviceName())) {
                return deviceHealthResponse.getHealthStatus();
            }
        }
        return new DeviceHealthResponse(keyLockDevice.getDeviceName(), DeviceHealth.NOTREADY).getHealthStatus();
    }
}

