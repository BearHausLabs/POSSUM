package com.target.devicemanager.components.toneindicator;

import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.entities.*;
import com.target.devicemanager.components.toneindicator.entities.ToneIndicatorError;
import com.target.devicemanager.components.toneindicator.entities.ToneRequest;
import jpos.JposException;
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
public class ToneIndicatorManager {

    @Autowired
    private CacheManager cacheManager;

    private final ToneIndicatorDevice toneIndicatorDevice;
    private final Lock toneIndicatorLock;
    private ConnectEnum connectStatus = ConnectEnum.FIRST_CONNECT;
    private static final Logger LOGGER = LoggerFactory.getLogger(ToneIndicatorManager.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("ToneIndicator", "ToneIndicatorManager", LOGGER);

    public ToneIndicatorManager(ToneIndicatorDevice toneIndicatorDevice, Lock toneIndicatorLock) {
        this(toneIndicatorDevice, toneIndicatorLock, null);
    }

    public ToneIndicatorManager(ToneIndicatorDevice toneIndicatorDevice, Lock toneIndicatorLock, CacheManager cacheManager) {
        if (toneIndicatorDevice == null) {
            throw new IllegalArgumentException("toneIndicatorDevice cannot be null");
        }
        if (toneIndicatorLock == null) {
            throw new IllegalArgumentException("toneIndicatorLock cannot be null");
        }
        this.toneIndicatorDevice = toneIndicatorDevice;
        this.toneIndicatorLock = toneIndicatorLock;

        if (cacheManager != null) {
            this.cacheManager = cacheManager;
        }
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void connect() {
        if (toneIndicatorDevice.tryLock()) {
            try {
                toneIndicatorDevice.connect();
            } finally {
                toneIndicatorDevice.unlock();
            }
        }

        if (connectStatus == ConnectEnum.FIRST_CONNECT) {
            connectStatus = ConnectEnum.CHECK_HEALTH;
        }
    }

    public void reconnectDevice() throws DeviceException {
        if (toneIndicatorDevice.tryLock()) {
            try {
                toneIndicatorDevice.disconnect();
                if (!toneIndicatorDevice.connect()) {
                    throw new DeviceException(DeviceError.DEVICE_OFFLINE);
                }
            } finally {
                toneIndicatorDevice.unlock();
            }
        } else {
            throw new DeviceException(DeviceError.DEVICE_BUSY);
        }
    }

    public void playSound(ToneRequest toneRequest) throws DeviceException {
        if (!toneIndicatorLock.tryLock()) {
            DeviceException deviceException = new DeviceException(DeviceError.DEVICE_BUSY);
            throw deviceException;
        }
        try {
            toneIndicatorDevice.playSound(toneRequest);
        } catch (JposException jposException) {
            DeviceException deviceException = new DeviceException(jposException);
            throw deviceException;
        } catch (DeviceException deviceException) {
            throw deviceException;
        } finally {
            toneIndicatorLock.unlock();
        }
    }

    public DeviceHealthResponse getHealth() {
        DeviceHealthResponse deviceHealthResponse;
        if (toneIndicatorDevice.isConnected()) {
            deviceHealthResponse = new DeviceHealthResponse(toneIndicatorDevice.getDeviceName(), DeviceHealth.READY);
        } else {
            deviceHealthResponse = new DeviceHealthResponse(toneIndicatorDevice.getDeviceName(), DeviceHealth.NOTREADY);
        }
        try {
            Objects.requireNonNull(cacheManager.getCache("toneIndicatorHealth")).put("health", deviceHealthResponse);
        } catch (Exception exception) {
            log.failure("getCache(toneIndicatorHealth) Failed", 17, exception);
        }
        return deviceHealthResponse;
    }

    public DeviceHealthResponse getStatus() {
        try {
            if (cacheManager != null && Objects.requireNonNull(cacheManager.getCache("toneIndicatorHealth")).get("health") != null) {
                if (connectStatus == ConnectEnum.CHECK_HEALTH) {
                    connectStatus = ConnectEnum.HEALTH_UPDATED;
                    return getHealth();
                }
                return (DeviceHealthResponse) Objects.requireNonNull(cacheManager.getCache("toneIndicatorHealth")).get("health").get();
            } else {
                log.success("Not able to retrieve from cache, checking getHealth()", 5);
                return getHealth();
            }
        } catch (Exception exception) {
            return getHealth();
        }
    }
}
