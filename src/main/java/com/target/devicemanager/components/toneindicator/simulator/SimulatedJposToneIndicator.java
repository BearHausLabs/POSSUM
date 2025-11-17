package com.target.devicemanager.components.toneindicator.simulator;

import jpos.*;
import jpos.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulated JavaPOS ToneIndicator implementation for testing without hardware.
 * Logs tone requests instead of playing actual sounds.
 */
public class SimulatedJposToneIndicator extends ToneIndicator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedJposToneIndicator.class);
    
    private boolean claimed = false;
    private boolean deviceEnabled = false;
    private int state = JposConst.JPOS_S_CLOSED;
    
    // Tone configuration
    private int tone1Pitch = 1000;
    private int tone1Duration = 200;
    private int tone1Volume = 100;
    private int tone2Pitch = 1500;
    private int tone2Duration = 300;
    private int tone2Volume = 100;
    private int interToneWait = 0;
    private boolean asyncMode = false;
    
    private final List<StatusUpdateListener> statusUpdateListeners = new ArrayList<>();
    
    public SimulatedJposToneIndicator() {
        LOGGER.info("SimulatedJposToneIndicator created");
    }
    
    @Override
    public void open(String logicalName) throws JposException {
        LOGGER.info("Simulated ToneIndicator open: {}", logicalName);
        state = JposConst.JPOS_S_IDLE;
    }
    
    @Override
    public void claim(int timeout) throws JposException {
        LOGGER.info("Simulated ToneIndicator claim: timeout={}", timeout);
        if (state == JposConst.JPOS_S_CLOSED) {
            throw new JposException(JposConst.JPOS_E_CLOSED, "Device not open");
        }
        claimed = true;
    }
    
    @Override
    public void release() throws JposException {
        LOGGER.info("Simulated ToneIndicator release");
        claimed = false;
    }
    
    @Override
    public void close() throws JposException {
        LOGGER.info("Simulated ToneIndicator close");
        deviceEnabled = false;
        claimed = false;
        state = JposConst.JPOS_S_CLOSED;
        statusUpdateListeners.clear();
    }
    
    @Override
    public boolean getClaimed() throws JposException {
        return claimed;
    }
    
    @Override
    public void setDeviceEnabled(boolean enabled) throws JposException {
        LOGGER.info("Simulated ToneIndicator setDeviceEnabled: {}", enabled);
        if (!claimed) {
            throw new JposException(JposConst.JPOS_E_NOTCLAIMED, "Device not claimed");
        }
        deviceEnabled = enabled;
    }
    
    @Override
    public boolean getDeviceEnabled() throws JposException {
        return deviceEnabled;
    }
    
    @Override
    public void setTone1Pitch(int pitch) throws JposException {
        LOGGER.debug("Simulated ToneIndicator setTone1Pitch: {}", pitch);
        this.tone1Pitch = pitch;
    }
    
    @Override
    public int getTone1Pitch() throws JposException {
        return tone1Pitch;
    }
    
    @Override
    public void setTone1Duration(int duration) throws JposException {
        LOGGER.debug("Simulated ToneIndicator setTone1Duration: {}", duration);
        this.tone1Duration = duration;
    }
    
    @Override
    public int getTone1Duration() throws JposException {
        return tone1Duration;
    }
    
    @Override
    public void setTone1Volume(int volume) throws JposException {
        LOGGER.debug("Simulated ToneIndicator setTone1Volume: {}", volume);
        this.tone1Volume = volume;
    }
    
    @Override
    public int getTone1Volume() throws JposException {
        return tone1Volume;
    }
    
    @Override
    public void setTone2Pitch(int pitch) throws JposException {
        LOGGER.debug("Simulated ToneIndicator setTone2Pitch: {}", pitch);
        this.tone2Pitch = pitch;
    }
    
    @Override
    public int getTone2Pitch() throws JposException {
        return tone2Pitch;
    }
    
    @Override
    public void setTone2Duration(int duration) throws JposException {
        LOGGER.debug("Simulated ToneIndicator setTone2Duration: {}", duration);
        this.tone2Duration = duration;
    }
    
    @Override
    public int getTone2Duration() throws JposException {
        return tone2Duration;
    }
    
    @Override
    public void setTone2Volume(int volume) throws JposException {
        LOGGER.debug("Simulated ToneIndicator setTone2Volume: {}", volume);
        this.tone2Volume = volume;
    }
    
    @Override
    public int getTone2Volume() throws JposException {
        return tone2Volume;
    }
    
    @Override
    public void setInterToneWait(int wait) throws JposException {
        LOGGER.debug("Simulated ToneIndicator setInterToneWait: {}", wait);
        this.interToneWait = wait;
    }
    
    @Override
    public int getInterToneWait() throws JposException {
        return interToneWait;
    }
    
    @Override
    public boolean getAsyncMode() throws JposException {
        return asyncMode;
    }
    
    @Override
    public void sound(int toneNumber, int repeatCount) throws JposException {
        if (!deviceEnabled) {
            throw new JposException(JposConst.JPOS_E_DISABLED, "Device not enabled");
        }
        
        int pitch = (toneNumber == 1) ? tone1Pitch : tone2Pitch;
        int duration = (toneNumber == 1) ? tone1Duration : tone2Duration;
        int volume = (toneNumber == 1) ? tone1Volume : tone2Volume;
        
        LOGGER.info("Simulated ToneIndicator sound: Tone{} - pitch={}Hz, duration={}ms, volume={}%, repeat={}, wait={}ms",
                   toneNumber, pitch, duration, volume, repeatCount, interToneWait);
        
        // Simulate the time it would take to play the tones
        try {
            for (int i = 0; i < repeatCount; i++) {
                Thread.sleep(duration);
                if (i < repeatCount - 1 && interToneWait > 0) {
                    Thread.sleep(interToneWait);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        LOGGER.debug("Simulated tone completed");
    }
    
    @Override
    public int getState() {
        return state;
    }
    
    @Override
    public String getPhysicalDeviceName() throws JposException {
        return "SimulatedToneIndicator";
    }
    
    @Override
    public String getPhysicalDeviceDescription() throws JposException {
        return "Simulated JavaPOS ToneIndicator for Testing";
    }
    
    @Override
    public int getDeviceServiceVersion() throws JposException {
        return 1014000; // Version 1.14
    }
    
    @Override
    public int getPowerState() throws JposException {
        return JposConst.JPOS_PS_ONLINE;
    }
    
    @Override
    public int getCapPowerReporting() throws JposException {
        return JposConst.JPOS_PR_STANDARD;
    }
    
    @Override
    public void addStatusUpdateListener(StatusUpdateListener listener) {
        statusUpdateListeners.add(listener);
    }
    
    @Override
    public void removeStatusUpdateListener(StatusUpdateListener listener) {
        statusUpdateListeners.remove(listener);
    }
}

