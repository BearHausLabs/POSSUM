package com.target.devicemanager.components.poskeyboard;

import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.entities.*;
import com.target.devicemanager.components.poskeyboard.entities.KeyboardEventData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;

@Profile({"local", "dev", "prod"})
@EnableScheduling
@EnableCaching
public class POSKeyboardManager {

    @Autowired
    private CacheManager cacheManager;

    private final POSKeyboardDevice posKeyboardDevice;
    private final Lock posKeyboardLock;
    private ConnectEnum connectStatus = ConnectEnum.FIRST_CONNECT;
    private final List<SseEmitter> eventSubscribers = new CopyOnWriteArrayList<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(POSKeyboardManager.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("POSKeyboard", "POSKeyboardManager", LOGGER);

    public POSKeyboardManager(POSKeyboardDevice posKeyboardDevice, Lock posKeyboardLock) {
        this(posKeyboardDevice, posKeyboardLock, null);
    }

    public POSKeyboardManager(POSKeyboardDevice posKeyboardDevice, Lock posKeyboardLock, CacheManager cacheManager) {
        if (posKeyboardDevice == null) {
            throw new IllegalArgumentException("posKeyboardDevice cannot be null");
        }
        if (posKeyboardLock == null) {
            throw new IllegalArgumentException("posKeyboardLock cannot be null");
        }
        this.posKeyboardDevice = posKeyboardDevice;
        this.posKeyboardLock = posKeyboardLock;

        if (cacheManager != null) {
            this.cacheManager = cacheManager;
        }

        // Register this manager as the event callback on the device
        this.posKeyboardDevice.setEventCallback(this::onKeyEvent);
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void connect() {
        if (posKeyboardDevice.tryLock()) {
            try {
                posKeyboardDevice.connect();
            } finally {
                posKeyboardDevice.unlock();
            }
        }

        if (connectStatus == ConnectEnum.FIRST_CONNECT) {
            connectStatus = ConnectEnum.CHECK_HEALTH;
        }
    }

    public void reconnectDevice() throws DeviceException {
        if (posKeyboardDevice.tryLock()) {
            try {
                posKeyboardDevice.disconnect();
                if (!posKeyboardDevice.connect()) {
                    throw new DeviceException(DeviceError.DEVICE_OFFLINE);
                }
            } finally {
                posKeyboardDevice.unlock();
            }
        } else {
            throw new DeviceException(DeviceError.DEVICE_BUSY);
        }
    }

    /**
     * Callback invoked by the device when a key event occurs.
     * Sends the event to all connected SSE subscribers.
     */
    private void onKeyEvent(KeyboardEventData event) {
        log.success("Key event received: " + event, 1);
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : eventSubscribers) {
            try {
                emitter.send(event, MediaType.APPLICATION_JSON);
            } catch (IOException ioException) {
                deadEmitters.add(emitter);
            }
        }
        eventSubscribers.removeAll(deadEmitters);
    }

    /**
     * Adds an SSE emitter to the subscriber list for keyboard events.
     */
    public void addEventSubscriber(SseEmitter emitter) {
        emitter.onCompletion(() -> eventSubscribers.remove(emitter));
        emitter.onTimeout(() -> eventSubscribers.remove(emitter));
        eventSubscribers.add(emitter);
        log.success("SSE subscriber added, total: " + eventSubscribers.size(), 5);
    }

    /**
     * Removes an SSE emitter from the subscriber list.
     */
    public void removeEventSubscriber(SseEmitter emitter) {
        eventSubscribers.remove(emitter);
        log.success("SSE subscriber removed, total: " + eventSubscribers.size(), 5);
    }

    public DeviceHealthResponse getHealth() {
        DeviceHealthResponse deviceHealthResponse;
        if (posKeyboardDevice.isConnected()) {
            deviceHealthResponse = new DeviceHealthResponse(posKeyboardDevice.getDeviceName(), DeviceHealth.READY);
        } else {
            deviceHealthResponse = new DeviceHealthResponse(posKeyboardDevice.getDeviceName(), DeviceHealth.NOTREADY);
        }
        try {
            Objects.requireNonNull(cacheManager.getCache("posKeyboardHealth")).put("health", deviceHealthResponse);
        } catch (Exception exception) {
            log.failure("getCache(posKeyboardHealth) Failed", 17, exception);
        }
        return deviceHealthResponse;
    }

    public DeviceHealthResponse getStatus() {
        try {
            if (cacheManager != null && Objects.requireNonNull(cacheManager.getCache("posKeyboardHealth")).get("health") != null) {
                if (connectStatus == ConnectEnum.CHECK_HEALTH) {
                    connectStatus = ConnectEnum.HEALTH_UPDATED;
                    return getHealth();
                }
                return (DeviceHealthResponse) Objects.requireNonNull(cacheManager.getCache("posKeyboardHealth")).get("health").get();
            } else {
                log.success("Not able to retrieve from cache, checking getHealth()", 5);
                return getHealth();
            }
        } catch (Exception exception) {
            return getHealth();
        }
    }
}
