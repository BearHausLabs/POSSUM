package com.target.devicemanager.components.cashdrawer;

import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.entities.*;
import com.target.devicemanager.components.cashdrawer.entities.CashDrawerError;
import jpos.JposException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

@Profile({"local", "dev", "prod"})
@EnableScheduling
@EnableCaching
public class CashDrawerManager {

    public static final int MIN_DRAWER_ID = 1;
    public static final int MAX_DRAWER_ID = 4;

    @Autowired
    private CacheManager cacheManager;

    private final Map<Integer, CashDrawerDevice> cashDrawerDevices;
    private final Lock cashDrawerLock;
    private ConnectEnum connectStatus = ConnectEnum.FIRST_CONNECT;
    private static final Logger LOGGER = LoggerFactory.getLogger(CashDrawerManager.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("CashDrawer", "CashDrawerManager", LOGGER);

    public CashDrawerManager(Map<Integer, CashDrawerDevice> cashDrawerDevices, Lock cashDrawerLock) {
        this(cashDrawerDevices, cashDrawerLock, null);
    }

    public CashDrawerManager(Map<Integer, CashDrawerDevice> cashDrawerDevices, Lock cashDrawerLock, CacheManager cacheManager) {
        if (cashDrawerDevices == null || cashDrawerDevices.isEmpty()) {
            throw new IllegalArgumentException("cashDrawerDevices cannot be null or empty");
        }
        if (cashDrawerLock == null) {
            throw new IllegalArgumentException("cashDrawerLock cannot be null");
        }
        this.cashDrawerDevices = cashDrawerDevices;
        this.cashDrawerLock = cashDrawerLock;

        if (cacheManager != null) {
            this.cacheManager = cacheManager;
        }
    }

    private CashDrawerDevice getDevice(int drawerId) throws DeviceException {
        if (drawerId < MIN_DRAWER_ID || drawerId > MAX_DRAWER_ID) {
            throw new DeviceException(DeviceError.DEVICE_OFFLINE);
        }
        CashDrawerDevice device = cashDrawerDevices.get(drawerId);
        if (device == null) {
            throw new DeviceException(DeviceError.DEVICE_OFFLINE);
        }
        return device;
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void connect() {
        for (CashDrawerDevice device : cashDrawerDevices.values()) {
            if (device.tryLock()) {
                try {
                    device.connect();
                } finally {
                    device.unlock();
                }
            }
        }

        if (connectStatus == ConnectEnum.FIRST_CONNECT) {
            connectStatus = ConnectEnum.CHECK_HEALTH;
        }
    }

    public void reconnectDevice(int drawerId) throws DeviceException {
        CashDrawerDevice cashDrawerDevice = getDevice(drawerId);
        if (cashDrawerDevice.tryLock()) {
            try {
                cashDrawerDevice.disconnect();
                if (!cashDrawerDevice.connect()) {
                    throw new DeviceException(DeviceError.DEVICE_OFFLINE);
                }
            } finally {
                cashDrawerDevice.unlock();
            }
        } else {
            throw new DeviceException(DeviceError.DEVICE_BUSY);
        }
    }

    public void openCashDrawer(int drawerId) throws DeviceException {
        if (!cashDrawerLock.tryLock()) {
            DeviceException cashDrawerException = new DeviceException(CashDrawerError.DEVICE_BUSY);
            throw cashDrawerException;
        }
        try {
            CashDrawerDevice cashDrawerDevice = getDevice(drawerId);
            cashDrawerDevice.openCashDrawer();
        } catch (JposException jposException) {
            DeviceException cashDrawerException = new DeviceException(jposException);
            throw cashDrawerException;
        } catch (DeviceException cashDrawerException) {
            throw cashDrawerException;
        } finally {
            cashDrawerLock.unlock();
        }
    }

    public DeviceHealthResponse getHealth(int drawerId) throws DeviceException {
        CashDrawerDevice cashDrawerDevice = getDevice(drawerId);
        DeviceHealthResponse deviceHealthResponse;
        if (cashDrawerDevice.isConnected()) {
            deviceHealthResponse = new DeviceHealthResponse(cashDrawerDevice.getDeviceName(), DeviceHealth.READY);
        } else {
            deviceHealthResponse = new DeviceHealthResponse(cashDrawerDevice.getDeviceName(), DeviceHealth.NOTREADY);
        }
        try {
            String cacheName = "cashDrawer" + drawerId + "Health";
            Objects.requireNonNull(cacheManager.getCache(cacheName)).put("health", deviceHealthResponse);
        } catch (Exception exception) {
            log.failure("getCache(cashDrawer" + drawerId + "Health) Failed", 17, exception);
        }
        return deviceHealthResponse;
    }

    public DeviceHealthResponse getStatus(int drawerId) throws DeviceException {
        try {
            String cacheName = "cashDrawer" + drawerId + "Health";
            if (cacheManager != null && Objects.requireNonNull(cacheManager.getCache(cacheName)).get("health") != null) {
                if (connectStatus == ConnectEnum.CHECK_HEALTH) {
                    connectStatus = ConnectEnum.HEALTH_UPDATED;
                    return getHealth(drawerId);
                }
                return (DeviceHealthResponse) Objects.requireNonNull(cacheManager.getCache(cacheName)).get("health").get();
            } else {
                log.success("Not able to retrieve from cache, checking getHealth()", 5);
                return getHealth(drawerId);
            }
        } catch (Exception exception) {
            return getHealth(drawerId);
        }
    }

    public List<DeviceHealthResponse> getAllHealth() {
        List<DeviceHealthResponse> responses = new ArrayList<>();
        for (Integer drawerId : cashDrawerDevices.keySet()) {
            try {
                responses.add(getHealth(drawerId));
            } catch (DeviceException e) {
                log.failure("getHealth for drawer " + drawerId + " failed", 17, e);
            }
        }
        return responses;
    }
}
