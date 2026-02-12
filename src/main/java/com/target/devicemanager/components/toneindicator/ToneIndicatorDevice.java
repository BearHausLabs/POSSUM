package com.target.devicemanager.components.toneindicator;

import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.components.toneindicator.entities.ToneIndicatorError;
import com.target.devicemanager.components.toneindicator.entities.ToneRequest;
import jpos.JposConst;
import jpos.JposException;
import jpos.ToneIndicator;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Profile({"local", "dev", "prod"})
public class ToneIndicatorDevice implements StatusUpdateListener {
    private final DynamicDevice<? extends ToneIndicator> dynamicToneIndicator;
    private final DeviceListener deviceListener;
    private boolean deviceConnected = false;
    private boolean areListenersAttached;
    private final ReentrantLock connectLock;
    private boolean isLocked = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(ToneIndicatorDevice.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("ToneIndicator", "ToneIndicatorDevice", LOGGER);

    public ToneIndicatorDevice(DynamicDevice<? extends ToneIndicator> dynamicToneIndicator, DeviceListener deviceListener) {
        this(dynamicToneIndicator, deviceListener, new ReentrantLock(true));
    }

    public ToneIndicatorDevice(DynamicDevice<? extends ToneIndicator> dynamicToneIndicator, DeviceListener deviceListener, ReentrantLock connectLock) {
        if (dynamicToneIndicator == null) {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException("dynamicToneIndicator cannot be null");
            log.failure("Tone Indicator Failed in Constructor: dynamicToneIndicator cannot be null", 18,
                    illegalArgumentException);
            throw illegalArgumentException;
        }
        if (deviceListener == null) {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException("deviceListener cannot be null");
            log.failure("Tone Indicator Failed in Constructor: deviceListener cannot be null", 18,
                    illegalArgumentException);
            throw illegalArgumentException;
        }
        this.dynamicToneIndicator = dynamicToneIndicator;
        this.deviceListener = deviceListener;
        this.connectLock = connectLock;
    }

    public boolean connect() {
        if (dynamicToneIndicator.connect() == DynamicDevice.ConnectionResult.NOT_CONNECTED) {
            return false;
        }
        if (!areListenersAttached) {
            attachEventListeners();
            areListenersAttached = true;
        }

        ToneIndicator toneIndicator;
        synchronized (toneIndicator = dynamicToneIndicator.getDevice()) {
            try {
                if (!toneIndicator.getDeviceEnabled()) {
                    toneIndicator.setDeviceEnabled(true);
                    deviceConnected = true;
                }
            } catch (JposException jposException) {
                deviceConnected = false;
                return false;
            }
        }
        return true;
    }

    public void setAreListenersAttached(boolean areListenersAttached) {
        this.areListenersAttached = areListenersAttached;
    }

    public boolean getAreListenersAttached() {
        return areListenersAttached;
    }

    public void setDeviceConnected(boolean deviceConnected) {
        this.deviceConnected = deviceConnected;
    }

    public void disconnect() {
        if (dynamicToneIndicator.isConnected()) {
            if (areListenersAttached) {
                detachEventListeners();
                areListenersAttached = false;
            }
            ToneIndicator toneIndicator;
            synchronized (toneIndicator = dynamicToneIndicator.getDevice()) {
                try {
                    if (toneIndicator.getDeviceEnabled()) {
                        toneIndicator.setDeviceEnabled(false);
                        dynamicToneIndicator.disconnect();
                        deviceConnected = false;
                    }
                } catch (JposException jposException) {
                    log.failure("Tone Indicator Failed to Disconnect", 18, jposException);
                }
            }
        }
    }

    public void playSound(ToneRequest toneRequest) throws JposException, DeviceException {
        enable();
        ToneIndicator toneIndicator;
        synchronized (toneIndicator = dynamicToneIndicator.getDevice()) {
            try {
                log.success("Playing tone: " + toneRequest, 1);

                // Configure tone 1
                toneIndicator.setTone1Pitch(toneRequest.pitch1);
                toneIndicator.setTone1Duration(toneRequest.duration1);
                toneIndicator.setTone1Volume(toneRequest.volume1);

                // Configure tone 2 if pitch2 is specified
                if (toneRequest.pitch2 > 0) {
                    toneIndicator.setTone2Pitch(toneRequest.pitch2);
                    toneIndicator.setTone2Duration(toneRequest.duration2);
                    toneIndicator.setTone2Volume(toneRequest.volume2);
                    toneIndicator.setInterToneWait(toneRequest.interToneWait);
                }

                // Play the tone synchronously (numberOfCycles=1, interSoundWait=0)
                int numberOfTones = toneRequest.pitch2 > 0 ? 2 : 1;
                toneIndicator.sound(numberOfTones, 0);

                log.success("Tone played successfully", 1);
            } catch (JposException jposException) {
                log.failure("Failed to play tone", 18, jposException);
                throw jposException;
            }
        }
    }

    private void enable() throws JposException {
        if (!isConnected()) {
            JposException jposException = new JposException(JposConst.JPOS_E_OFFLINE);
            log.failure("Tone Indicator is not connected", 18, jposException);
            throw jposException;
        }
        deviceListener.startEventListeners();
    }

    public String getDeviceName() {
        return dynamicToneIndicator.getDeviceName();
    }

    public boolean isConnected() {
        return deviceConnected;
    }

    private void attachEventListeners() {
        ToneIndicator toneIndicator;
        synchronized (toneIndicator = dynamicToneIndicator.getDevice()) {
            toneIndicator.addStatusUpdateListener(this);
        }
    }

    private void detachEventListeners() {
        ToneIndicator toneIndicator;
        synchronized (toneIndicator = dynamicToneIndicator.getDevice()) {
            toneIndicator.removeStatusUpdateListener(this);
        }
    }

    @Override
    public void statusUpdateOccurred(StatusUpdateEvent statusUpdateEvent) {
        int status = statusUpdateEvent.getStatus();
        log.success("Tone Indicator statusUpdateOccurred(): " + status, 1);
        switch (status) {
            case JposConst.JPOS_SUE_POWER_OFF:
            case JposConst.JPOS_SUE_POWER_OFF_OFFLINE:
            case JposConst.JPOS_SUE_POWER_OFFLINE:
                log.failure("Tone Indicator Status Update: Power offline", 13, null);
                deviceConnected = false;
                break;
            case JposConst.JPOS_SUE_POWER_ONLINE:
                log.success("Status Update: Power online", 5);
                deviceConnected = true;
                break;
            default:
                break;
        }
    }

    public boolean tryLock() {
        try {
            isLocked = connectLock.tryLock(10, TimeUnit.SECONDS);
            log.success("Lock: " + isLocked, 1);
        } catch (InterruptedException interruptedException) {
            log.failure("Lock Failed", 17, interruptedException);
        }
        return isLocked;
    }

    public void unlock() {
        connectLock.unlock();
        isLocked = false;
    }

    public boolean getIsLocked() {
        return isLocked;
    }
}
