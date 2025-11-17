package com.target.devicemanager.components.poskeyboard;

import com.target.devicemanager.common.entities.*;
import com.target.devicemanager.components.poskeyboard.entities.POSKeyData;
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
import java.util.concurrent.locks.Lock;

/**
 * Manager for POSKeyboard device operations.
 * Handles device lifecycle, health monitoring, and key reading operations.
 */
@EnableScheduling
@EnableCaching
public class POSKeyboardManager {
    
    @Autowired
    private CacheManager cacheManager;
    
    private final POSKeyboardDevice posKeyboardDevice;
    private ConnectEnum connectStatus = ConnectEnum.FIRST_CONNECT;
    private static final Logger LOGGER = LoggerFactory.getLogger(POSKeyboardManager.class);
    
    public POSKeyboardManager(POSKeyboardDevice posKeyboardDevice, Lock keyboardLock) {
        this(posKeyboardDevice, keyboardLock, null);
    }
    
    public POSKeyboardManager(POSKeyboardDevice posKeyboardDevice, Lock keyboardLock, CacheManager cacheManager) {
        if (posKeyboardDevice == null) {
            throw new IllegalArgumentException("posKeyboardDevice cannot be null");
        }
        if (keyboardLock == null) {
            throw new IllegalArgumentException("keyboardLock cannot be null");
        }
        this.posKeyboardDevice = posKeyboardDevice;
        
        if (cacheManager != null) {
            this.cacheManager = cacheManager;
        }
    }
    
    /**
     * Scheduled connection check - runs every 5 seconds after initial 5 second delay.
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void connect() {
        posKeyboardDevice.connect();
        
        if (connectStatus == ConnectEnum.FIRST_CONNECT) {
            if (!posKeyboardDevice.isConnected()) {
                LOGGER.error("POSKeyboard Failed to Connect");
            }
            connectStatus = ConnectEnum.CHECK_HEALTH;
        }
    }
    
    /**
     * Reconnects the POSKeyboard device.
     * 
     * @throws DeviceException if reconnection fails
     */
    public void reconnectKeyboard() throws DeviceException {
        try {
            if (!posKeyboardDevice.reconnect()) {
                LOGGER.error("Failed to reconnect POSKeyboard.");
                throw new DeviceException(DeviceError.DEVICE_OFFLINE);
            }
        } catch (DeviceException e) {
            throw e;
        }
    }
    
    /**
     * Reads a key from the POSKeyboard with timeout.
     * 
     * @param timeoutMs Timeout in milliseconds
     * @return POSKeyData or null if timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public POSKeyData readKey(long timeoutMs) throws InterruptedException {
        LOGGER.trace("readKey(in)");
        try {
            return posKeyboardDevice.getPOSKeyData(timeoutMs);
        } finally {
            LOGGER.trace("readKey(out)");
        }
    }
    
    /**
     * Clears any pending key data.
     */
    public void clearKeyData() {
        posKeyboardDevice.clearKeyData();
    }
    
    /**
     * Gets the health status of the POSKeyboard.
     * 
     * @return List of device health responses
     */
    public List<DeviceHealthResponse> getHealth() {
        LOGGER.trace("getHealth(in)");
        List<DeviceHealthResponse> response = new ArrayList<>();
        
        if (posKeyboardDevice.isConnected()) {
            response.add(new DeviceHealthResponse(posKeyboardDevice.getDeviceName(), DeviceHealth.READY));
        } else {
            response.add(new DeviceHealthResponse(posKeyboardDevice.getDeviceName(), DeviceHealth.NOTREADY));
        }
        
        try {
            Objects.requireNonNull(cacheManager.getCache("posKeyboardHealth")).put("health", response);
        } catch (Exception exception) {
            LOGGER.error("getCache(posKeyboardHealth) Failed: " + exception.getMessage());
        }
        LOGGER.trace("getHealth(out)");
        return response;
    }
    
    /**
     * Gets the cached health status of the POSKeyboard.
     * 
     * @return List of device health responses
     */
    @SuppressWarnings("unchecked")
    public List<DeviceHealthResponse> getStatus() {
        try {
            if (cacheManager != null && Objects.requireNonNull(cacheManager.getCache("posKeyboardHealth")).get("health") != null) {
                if (connectStatus == ConnectEnum.CHECK_HEALTH) {
                    connectStatus = ConnectEnum.HEALTH_UPDATED;
                    return getHealth();
                }
                return (List<DeviceHealthResponse>) Objects.requireNonNull(cacheManager.getCache("posKeyboardHealth")).get("health").get();
            } else {
                LOGGER.debug("Not able to retrieve from cache, checking getHealth()");
                return getHealth();
            }
        } catch (Exception exception) {
            return getHealth();
        }
    }
    
    /**
     * Gets the health status of the POSKeyboard device.
     * 
     * @return DeviceHealth status
     */
    public DeviceHealth getKeyboardHealthStatus() {
        for (DeviceHealthResponse deviceHealthResponse : getStatus()) {
            if (deviceHealthResponse.getDeviceName().equals(posKeyboardDevice.getDeviceName())) {
                return deviceHealthResponse.getHealthStatus();
            }
        }
        return new DeviceHealthResponse(posKeyboardDevice.getDeviceName(), DeviceHealth.NOTREADY).getHealthStatus();
    }
}

