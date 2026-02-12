package com.target.devicemanager.components.keylock;

import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.entities.*;
import com.target.devicemanager.components.keylock.entities.KeylockPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;
import java.util.concurrent.locks.Lock;

@Profile({"local", "dev", "prod"})
@EnableScheduling
@EnableCaching
public class KeylockManager {

    @Autowired
    private CacheManager cacheManager;

    private final KeylockDevice keylockDevice;
    private final Lock keylockLock;
    private ConnectEnum connectStatus = ConnectEnum.FIRST_CONNECT;
    private static final Logger LOGGER = LoggerFactory.getLogger(KeylockManager.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("Keylock", "KeylockManager", LOGGER);

    public KeylockManager(KeylockDevice keylockDevice, Lock keylockLock) {
        this(keylockDevice, keylockLock, null);
    }

    public KeylockManager(KeylockDevice keylockDevice, Lock keylockLock, CacheManager cacheManager) {
        if (keylockDevice == null) {
            throw new IllegalArgumentException("keylockDevice cannot be null");
        }
        if (keylockLock == null) {
            throw new IllegalArgumentException("keylockLock cannot be null");
        }
        this.keylockDevice = keylockDevice;
        this.keylockLock = keylockLock;

        if(cacheManager != null) {
            this.cacheManager = cacheManager;
        }
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void connect() {
        if (keylockDevice.tryLock()) {
            try {
                keylockDevice.connect();
            } finally {
                keylockDevice.unlock();
            }
        }

        if (connectStatus == ConnectEnum.FIRST_CONNECT) {
            connectStatus = ConnectEnum.CHECK_HEALTH;
        }
    }

    public void reconnectDevice() throws DeviceException {
        if (keylockDevice.tryLock()) {
            try {
                keylockDevice.disconnect();
                if (!keylockDevice.connect()) {
                    throw new DeviceException(DeviceError.DEVICE_OFFLINE);
                }
            } finally {
                keylockDevice.unlock();
            }
        }
        else {
            throw new DeviceException(DeviceError.DEVICE_BUSY);
        }
    }

    public KeylockPosition getKeyPosition() throws DeviceException {
        if (!keylockLock.tryLock()) {
            throw new DeviceException(DeviceError.DEVICE_BUSY);
        }
        try {
            if (!keylockDevice.isConnected()) {
                throw new DeviceException(DeviceError.DEVICE_OFFLINE);
            }
            return keylockDevice.getKeyPosition();
        } finally {
            keylockLock.unlock();
        }
    }

    public DeviceHealthResponse getHealth() {
        DeviceHealthResponse deviceHealthResponse;
        if (keylockDevice.isConnected()) {
            deviceHealthResponse = new DeviceHealthResponse(keylockDevice.getDeviceName(), DeviceHealth.READY);
        } else {
            deviceHealthResponse = new DeviceHealthResponse(keylockDevice.getDeviceName(), DeviceHealth.NOTREADY);
        }
        try {
            Objects.requireNonNull(cacheManager.getCache("keylockHealth")).put("health", deviceHealthResponse);
        } catch (Exception exception) {
            log.failure("getCache(keylockHealth) Failed", 17, exception);
        }
        return deviceHealthResponse;
    }

    public DeviceHealthResponse getStatus() {
        try {
            if (cacheManager != null && Objects.requireNonNull(cacheManager.getCache("keylockHealth")).get("health") != null) {
                if (connectStatus == ConnectEnum.CHECK_HEALTH) {
                    connectStatus = ConnectEnum.HEALTH_UPDATED;
                    return getHealth();
                }
                return (DeviceHealthResponse) Objects.requireNonNull(cacheManager.getCache("keylockHealth")).get("health").get();
            } else {
                log.success("Not able to retrieve from cache, checking getHealth()", 5);
                return getHealth();
            }
        } catch (Exception exception) {
            return getHealth();
        }
    }
}
