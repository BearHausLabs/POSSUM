package com.target.devicemanager.components.toneindicator;

import com.target.devicemanager.common.entities.*;
import com.target.devicemanager.components.toneindicator.entities.ToneRequest;
import jpos.JposException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manager for ToneIndicator device operations.
 * Handles device lifecycle, health monitoring, and sound operations.
 */
@EnableScheduling
@EnableCaching
public class ToneIndicatorManager {
    
    @Autowired
    private CacheManager cacheManager;
    
    private final ToneIndicatorDevice toneIndicatorDevice;
    private ConnectEnum connectStatus = ConnectEnum.FIRST_CONNECT;
    private static final Logger LOGGER = LoggerFactory.getLogger(ToneIndicatorManager.class);
    
    @Value("${toneindicator.defaultFrequency:1000}")
    private int defaultFrequency;
    
    @Value("${toneindicator.defaultDuration:200}")
    private int defaultDuration;
    
    public ToneIndicatorManager(ToneIndicatorDevice toneIndicatorDevice) {
        this(toneIndicatorDevice, null);
    }
    
    public ToneIndicatorManager(ToneIndicatorDevice toneIndicatorDevice, CacheManager cacheManager) {
        if (toneIndicatorDevice == null) {
            throw new IllegalArgumentException("toneIndicatorDevice cannot be null");
        }
        this.toneIndicatorDevice = toneIndicatorDevice;
        
        if (cacheManager != null) {
            this.cacheManager = cacheManager;
        }
    }
    
    /**
     * Scheduled connection check - runs every 5 seconds after initial 5 second delay.
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void connect() {
        toneIndicatorDevice.connect();
        
        if (connectStatus == ConnectEnum.FIRST_CONNECT) {
            if (!toneIndicatorDevice.isConnected()) {
                LOGGER.error("ToneIndicator Failed to Connect");
            }
            connectStatus = ConnectEnum.CHECK_HEALTH;
        }
    }
    
    /**
     * Reconnects the ToneIndicator device.
     * 
     * @throws DeviceException if reconnection fails
     */
    public void reconnectToneIndicator() throws DeviceException {
        try {
            if (!toneIndicatorDevice.reconnect()) {
                LOGGER.error("Failed to reconnect ToneIndicator.");
                throw new DeviceException(DeviceError.DEVICE_OFFLINE);
            }
        } catch (DeviceException e) {
            throw e;
        }
    }
    
    /**
     * Plays a tone with custom parameters.
     * 
     * @param toneRequest The tone parameters
     * @throws JposException if the sound cannot be played
     */
    public void sound(ToneRequest toneRequest) throws JposException {
        LOGGER.trace("sound(in)");
        try {
            toneIndicatorDevice.sound(
                toneRequest.getFrequency(),
                toneRequest.getDuration(),
                toneRequest.getNumberOfCycles(),
                toneRequest.getInterSoundWait()
            );
        } finally {
            LOGGER.trace("sound(out)");
        }
    }
    
    /**
     * Plays an immediate default beep sound.
     * 
     * @throws JposException if the sound cannot be played
     */
    public void soundImmediate() throws JposException {
        LOGGER.trace("soundImmediate(in)");
        try {
            toneIndicatorDevice.soundImmediate();
        } finally {
            LOGGER.trace("soundImmediate(out)");
        }
    }
    
    /**
     * Gets the health status of the ToneIndicator.
     * 
     * @return List of device health responses
     */
    public List<DeviceHealthResponse> getHealth() {
        LOGGER.trace("getHealth(in)");
        List<DeviceHealthResponse> response = new ArrayList<>();
        
        if (toneIndicatorDevice.isConnected()) {
            response.add(new DeviceHealthResponse(toneIndicatorDevice.getDeviceName(), DeviceHealth.READY));
        } else {
            response.add(new DeviceHealthResponse(toneIndicatorDevice.getDeviceName(), DeviceHealth.NOTREADY));
        }
        
        try {
            Objects.requireNonNull(cacheManager.getCache("toneIndicatorHealth")).put("health", response);
        } catch (Exception exception) {
            LOGGER.error("getCache(toneIndicatorHealth) Failed: " + exception.getMessage());
        }
        LOGGER.trace("getHealth(out)");
        return response;
    }
    
    /**
     * Gets the cached health status of the ToneIndicator.
     * 
     * @return List of device health responses
     */
    @SuppressWarnings("unchecked")
    public List<DeviceHealthResponse> getStatus() {
        try {
            if (cacheManager != null && Objects.requireNonNull(cacheManager.getCache("toneIndicatorHealth")).get("health") != null) {
                if (connectStatus == ConnectEnum.CHECK_HEALTH) {
                    connectStatus = ConnectEnum.HEALTH_UPDATED;
                    return getHealth();
                }
                return (List<DeviceHealthResponse>) Objects.requireNonNull(cacheManager.getCache("toneIndicatorHealth")).get("health").get();
            } else {
                LOGGER.debug("Not able to retrieve from cache, checking getHealth()");
                return getHealth();
            }
        } catch (Exception exception) {
            return getHealth();
        }
    }
    
    /**
     * Gets the health status of the ToneIndicator device.
     * 
     * @return DeviceHealth status
     */
    public DeviceHealth getToneIndicatorHealthStatus() {
        for (DeviceHealthResponse deviceHealthResponse : getStatus()) {
            if (deviceHealthResponse.getDeviceName().equals(toneIndicatorDevice.getDeviceName())) {
                return deviceHealthResponse.getHealthStatus();
            }
        }
        return new DeviceHealthResponse(toneIndicatorDevice.getDeviceName(), DeviceHealth.NOTREADY).getHealthStatus();
    }
}

