package com.target.devicemanager.components.msr;

import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.entities.*;
import com.target.devicemanager.components.msr.entities.CardData;
import com.target.devicemanager.components.msr.entities.MSRError;
import com.target.devicemanager.components.msr.entities.MSRException;
import jpos.JposException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;
import java.util.concurrent.locks.Lock;

@EnableScheduling
@EnableCaching
public class MSRManager {

    @Autowired
    private CacheManager cacheManager;

    private final MSRDevice msrDevice;
    private final Lock msrLock;
    private ConnectEnum connectStatus = ConnectEnum.FIRST_CONNECT;
    private static final Logger LOGGER = LoggerFactory.getLogger(MSRManager.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("MSR", "MSRManager", LOGGER);

    public MSRManager(MSRDevice msrDevice, Lock msrLock) {
        this(msrDevice, msrLock, null);
    }

    public MSRManager(MSRDevice msrDevice, Lock msrLock, CacheManager cacheManager) {
        if (msrDevice == null) {
            throw new IllegalArgumentException("msrDevice cannot be null");
        }
        if (msrLock == null) {
            throw new IllegalArgumentException("msrLock cannot be null");
        }
        this.msrDevice = msrDevice;
        this.msrLock = msrLock;

        if (cacheManager != null) {
            this.cacheManager = cacheManager;
        }
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void connect() {
        msrDevice.connect();

        if (connectStatus == ConnectEnum.FIRST_CONNECT) {
            if (!msrDevice.isConnected()) {
                log.failure("MSR Failed to Connect", 17, null);
            }
            connectStatus = ConnectEnum.CHECK_HEALTH;
        }
    }

    public void reconnectDevice() throws DeviceException {
        if (msrDevice.tryLock()) {
            try {
                if (!msrDevice.reconnect()) {
                    log.failure("Failed to reconnect MSR.", 17, null);
                    throw new DeviceException(DeviceError.DEVICE_OFFLINE);
                }
            } finally {
                msrDevice.unlock();
            }
        } else {
            throw new DeviceException(DeviceError.DEVICE_BUSY);
        }
    }

    /**
     * Initiates a blocking card read. Acquires lock, waits for card swipe, returns data.
     */
    CardData getData() throws MSRException {
        log.success("getData(in)", 1);
        if (!msrLock.tryLock()) {
            MSRException msrException = new MSRException(MSRError.DEVICE_BUSY);
            log.success("getData(out) - device busy", 1);
            throw msrException;
        }
        try {
            return readCard();
        } finally {
            msrLock.unlock();
            log.success("getData(out)", 1);
        }
    }

    private CardData readCard() throws MSRException {
        log.success("readCard(in)", 1);
        try {
            CardData cardData = msrDevice.getCardData();
            log.success("readCard(out)", 1);
            return cardData;
        } catch (JposException jposException) {
            MSRException msrException = new MSRException(jposException);
            log.success("readCard(): " + msrException.getDeviceError().getDescription(), 1);
            log.success("readCard(out)", 1);
            throw msrException;
        }
    }

    /**
     * Cancels the current card read request.
     */
    void cancelReadRequest() throws MSRException {
        log.success("cancelReadRequest(in)", 1);
        if (msrLock.tryLock()) {
            try {
                MSRException msrException = new MSRException(MSRError.ALREADY_DISABLED);
                log.success("cancelReadRequest(out) - already disabled", 1);
                throw msrException;
            } finally {
                msrLock.unlock();
            }
        }
        msrDevice.cancelCardData();
        log.success("cancelReadRequest(out)", 1);
    }

    public DeviceHealthResponse getHealth() {
        log.success("getHealth(in)", 1);
        DeviceHealthResponse deviceHealthResponse;
        if (msrDevice.isConnected()) {
            deviceHealthResponse = new DeviceHealthResponse(msrDevice.getDeviceName(), DeviceHealth.READY);
        } else {
            deviceHealthResponse = new DeviceHealthResponse(msrDevice.getDeviceName(), DeviceHealth.NOTREADY);
        }
        try {
            Objects.requireNonNull(cacheManager.getCache("msrHealth")).put("health", deviceHealthResponse);
        } catch (Exception exception) {
            log.failure("getCache(msrHealth) Failed: " + exception.getMessage(), 17, exception);
        }
        log.success("getHealth(out)", 1);
        return deviceHealthResponse;
    }

    public DeviceHealthResponse getStatus() {
        try {
            if (cacheManager != null && Objects.requireNonNull(cacheManager.getCache("msrHealth")).get("health") != null) {
                if (connectStatus == ConnectEnum.CHECK_HEALTH) {
                    connectStatus = ConnectEnum.HEALTH_UPDATED;
                    return getHealth();
                }
                return (DeviceHealthResponse) Objects.requireNonNull(cacheManager.getCache("msrHealth")).get("health").get();
            } else {
                log.success("Not able to retrieve from cache, checking getHealth()", 6);
                return getHealth();
            }
        } catch (Exception exception) {
            return getHealth();
        }
    }
}
