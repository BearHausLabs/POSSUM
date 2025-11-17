package com.target.devicemanager.components.toneindicator;

import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import jpos.JposConst;
import jpos.JposException;
import jpos.ToneIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ToneIndicator device implementation following POSSUM patterns.
 * Integrates JavaPOS ToneIndicator with Spring Boot REST APIs.
 * No event listeners needed - synchronous device.
 */
public class ToneIndicatorDevice {
    
    private final DynamicDevice<? extends ToneIndicator> dynamicToneIndicator;
    private boolean deviceConnected = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(ToneIndicatorDevice.class);
    private static final Marker MARKER = MarkerFactory.getMarker("FATAL");
    private final ReentrantLock connectLock;
    private boolean isLocked = false;
    
    /**
     * Initializes ToneIndicator device.
     * 
     * @param dynamicToneIndicator the dynamic device wrapper
     */
    public ToneIndicatorDevice(DynamicDevice<? extends ToneIndicator> dynamicToneIndicator) {
        this(dynamicToneIndicator, new ReentrantLock(true));
    }
    
    public ToneIndicatorDevice(DynamicDevice<? extends ToneIndicator> dynamicToneIndicator,
                               ReentrantLock connectLock) {
        if (dynamicToneIndicator == null) {
            LOGGER.error(MARKER, "ToneIndicator Failed in Constructor: dynamicToneIndicator cannot be null");
            throw new IllegalArgumentException("dynamicToneIndicator cannot be null");
        }
        this.dynamicToneIndicator = dynamicToneIndicator;
        this.connectLock = connectLock;
    }
    
    /**
     * Makes sure a connection occurs.
     */
    public void connect() {
        if (tryLock()) {
            try {
                DynamicDevice.ConnectionResult connectionResult = dynamicToneIndicator.connect();
                if (connectionResult == DynamicDevice.ConnectionResult.CONNECTED) {
                    // CRITICAL: Sleep after open for device initialization
                    try {
                        Thread.sleep(1000);
                        LOGGER.debug("Waited 1 second for ToneIndicator device initialization");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    enable();
                    deviceConnected = true;
                } else if (connectionResult == DynamicDevice.ConnectionResult.NOT_CONNECTED) {
                    deviceConnected = false;
                } else {
                    deviceConnected = (connectionResult == DynamicDevice.ConnectionResult.ALREADY_CONNECTED);
                }
            } catch (JposException e) {
                LOGGER.error("Failed to enable ToneIndicator after connection", e);
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
                    dynamicToneIndicator.disconnect();
                }
                
                DynamicDevice.ConnectionResult connectionResult = dynamicToneIndicator.connect();
                if (connectionResult == DynamicDevice.ConnectionResult.CONNECTED) {
                    // CRITICAL: Sleep after open
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    enable();
                }
                deviceConnected = (connectionResult == DynamicDevice.ConnectionResult.CONNECTED || 
                                 connectionResult == DynamicDevice.ConnectionResult.ALREADY_CONNECTED);
            } catch (JposException e) {
                LOGGER.error("Failed to enable ToneIndicator after reconnection", e);
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
     * Plays a tone with specified parameters.
     * Uses JavaPOS ToneIndicator Tone1 configuration.
     * 
     * @param frequency Frequency in Hz
     * @param duration Duration in milliseconds
     * @param numberOfCycles Number of times to repeat
     * @param interSoundWait Time between repetitions in milliseconds
     * @throws JposException if the tone cannot be played
     */
    public void sound(int frequency, int duration, int numberOfCycles, int interSoundWait) 
            throws JposException {
        LOGGER.trace("ToneIndicator sound(in)");
        if (!isConnected()) {
            throw new JposException(JposConst.JPOS_E_OFFLINE);
        }
        
        LOGGER.info("Playing tone: freq={}Hz, duration={}ms, cycles={}, wait={}ms", 
                   frequency, duration, numberOfCycles, interSoundWait);
        
        try {
            ToneIndicator toneIndicator;
            synchronized (toneIndicator = dynamicToneIndicator.getDevice()) {
                // Configure Tone1 with the desired parameters
                toneIndicator.setTone1Pitch(frequency);
                toneIndicator.setTone1Duration(duration);
                toneIndicator.setTone1Volume(100); // Full volume
                toneIndicator.setInterToneWait(interSoundWait);
                
                // Play Tone1 for the specified number of cycles
                // sound(toneNumber, repeatCount) where toneNumber: 1=Tone1, 2=Tone2
                toneIndicator.sound(1, numberOfCycles);
            }
        } catch (JposException jposException) {
            if (isConnected()) {
                LOGGER.error(MARKER, "ToneIndicator Failed to Play Sound: {} ({})", 
                           jposException.getErrorCode(), jposException.getErrorCodeExtended());
            } else {
                LOGGER.error("ToneIndicator Failed to Play Sound: {} ({})", 
                           jposException.getErrorCode(), jposException.getErrorCodeExtended());
            }
            throw jposException;
        }
        LOGGER.debug("Tone played successfully");
        LOGGER.trace("ToneIndicator sound(out)");
    }
    
    /**
     * Plays an immediate default beep sound.
     * Uses a standard 1000Hz tone for 200ms.
     * 
     * @throws JposException if the sound cannot be played
     */
    public void soundImmediate() throws JposException {
        sound(1000, 200, 1, 0);
    }
    
    /**
     * Gets the device name.
     * 
     * @return device name
     */
    public String getDeviceName() {
        return dynamicToneIndicator.getDeviceName();
    }
    
    /**
     * Makes sure ToneIndicator is connected.
     * 
     * @return Connection status
     */
    public boolean isConnected() {
        return dynamicToneIndicator.isConnected();
    }
    
    /**
     * Makes sure ToneIndicator is connected and enabled.
     * 
     * @throws JposException if enabling fails
     */
    protected void enable() throws JposException {
        LOGGER.trace("ToneIndicator enable(in)");
        if (!isConnected()) {
            throw new JposException(JposConst.JPOS_E_OFFLINE);
        }
        
        try {
            ToneIndicator toneIndicator;
            synchronized (toneIndicator = dynamicToneIndicator.getDevice()) {
                toneIndicator.setDeviceEnabled(true);
            }
        } catch (JposException jposException) {
            if (isConnected()) {
                LOGGER.error(MARKER, "ToneIndicator Failed to Enable Device: {} ({})", 
                           jposException.getErrorCode(), jposException.getErrorCodeExtended());
            } else {
                LOGGER.error("ToneIndicator Failed to Enable Device: {} ({})", 
                           jposException.getErrorCode(), jposException.getErrorCodeExtended());
            }
            throw jposException;
        }
        LOGGER.info("ToneIndicator enabled");
        LOGGER.trace("ToneIndicator enable(out)");
    }
    
    /**
     * Lock the current resource.
     * 
     * @return true if lock acquired
     */
    public boolean tryLock() {
        try {
            isLocked = connectLock.tryLock(10, TimeUnit.SECONDS);
            LOGGER.trace("ToneIndicator Lock: " + isLocked);
        } catch (InterruptedException interruptedException) {
            LOGGER.error("ToneIndicator Lock Failed: " + interruptedException.getMessage());
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

