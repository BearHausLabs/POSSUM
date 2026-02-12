package com.target.devicemanager.components.msr;

import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.components.msr.entities.CardData;
import jpos.JposConst;
import jpos.JposException;
import jpos.MSR;
import jpos.events.DataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class MSRDevice {
    private final DynamicDevice<? extends MSR> dynamicMSR;
    private final DeviceListener deviceListener;
    private boolean deviceConnected = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(MSRDevice.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("MSR", "MSRDevice", LOGGER);
    private final ReentrantLock connectLock;
    private boolean isLocked = false;

    public MSRDevice(DeviceListener deviceListener, DynamicDevice<? extends MSR> dynamicMSR) {
        this(deviceListener, dynamicMSR, new ReentrantLock(true));
    }

    public MSRDevice(DeviceListener deviceListener, DynamicDevice<? extends MSR> dynamicMSR, ReentrantLock connectLock) {
        if (deviceListener == null) {
            log.failure("MSR Failed in Constructor: deviceListener cannot be null", 17, null);
            throw new IllegalArgumentException("deviceListener cannot be null");
        }
        if (dynamicMSR == null) {
            log.failure("MSR Failed in Constructor: dynamicMSR cannot be null", 17, null);
            throw new IllegalArgumentException("dynamicMSR cannot be null");
        }
        this.dynamicMSR = dynamicMSR;
        this.deviceListener = deviceListener;
        this.connectLock = connectLock;
    }

    /**
     * Makes sure a connection occurs.
     */
    public void connect() {
        if (tryLock()) {
            try {
                DynamicDevice.ConnectionResult connectionResult = dynamicMSR.connect();
                if (connectionResult == DynamicDevice.ConnectionResult.CONNECTED) {
                    attachEventListeners();
                    deviceConnected = true;
                } else if (connectionResult == DynamicDevice.ConnectionResult.NOT_CONNECTED) {
                    deviceConnected = false;
                } else {
                    deviceConnected = (connectionResult == DynamicDevice.ConnectionResult.ALREADY_CONNECTED);
                }
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
                    dynamicMSR.disconnect();
                    detachEventListeners();
                }
                DynamicDevice.ConnectionResult connectionResult = dynamicMSR.connect();
                if (connectionResult == DynamicDevice.ConnectionResult.CONNECTED) {
                    attachEventListeners();
                }
                deviceConnected = (connectionResult == DynamicDevice.ConnectionResult.CONNECTED || connectionResult == DynamicDevice.ConnectionResult.ALREADY_CONNECTED);
            } finally {
                unlock();
            }
        } else {
            throw new DeviceException(DeviceError.DEVICE_BUSY);
        }
        return deviceConnected;
    }

    /**
     * Gets the card data from the MSR.
     * Blocks until a card is swiped or the operation is cancelled.
     * @return CardData containing track data
     * @throws JposException if a device error occurs
     */
    public CardData getCardData() throws JposException {
        log.success("getCardData(in)", 1);
        enable();
        try {
            DataEvent dataEvent = deviceListener.waitForData();
            return handleDataEvent(dataEvent);
        } catch (JposException jposException) {
            throw jposException;
        }
    }

    /**
     * Handles the data event from the MSR and extracts track data.
     */
    private CardData handleDataEvent(DataEvent dataEvent) throws JposException {
        if (!(dataEvent.getSource() instanceof MSR)) {
            log.success("getCardData(out)", 1);
            JposException jposException = new JposException(JposConst.JPOS_E_FAILURE);
            log.failure("Failed to Handle Data: " + jposException.getMessage(), 17, jposException);
            throw jposException;
        }
        try {
            String track1;
            String track2;
            String track3;
            String track4;
            MSR msr;
            synchronized (msr = (MSR) dataEvent.getSource()) {
                track1 = new String(msr.getTrack1Data(), Charset.defaultCharset());
                track2 = new String(msr.getTrack2Data(), Charset.defaultCharset());
                track3 = new String(msr.getTrack3Data(), Charset.defaultCharset());
                track4 = new String(msr.getTrack4Data(), Charset.defaultCharset());
            }
            CardData cardData = new CardData(track1, track2, track3, track4);
            log.success("Returning card data: " + cardData, 9);
            log.success("getCardData(out)", 1);
            return cardData;
        } catch (JposException jposException) {
            log.failure("Failed to Handle Data: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended(), 17, jposException);
            throw jposException;
        }
    }

    /**
     * Disables MSR and cancels the current card read.
     * @return null
     */
    public Void cancelCardData() {
        log.success("cancelCardData(in)", 1);
        try {
            disable();
        } catch (JposException jposException) {
            log.failure("Received exception in cancelCardData", 17, jposException);
        } finally {
            deviceListener.stopWaitingForData();
        }
        log.success("cancelCardData(out)", 1);
        return null;
    }

    /**
     * Gets the device name.
     */
    public String getDeviceName() {
        return dynamicMSR.getDeviceName();
    }

    /**
     * Makes sure MSR is connected.
     */
    public boolean isConnected() {
        return dynamicMSR.isConnected();
    }

    /**
     * Makes sure MSR is connected and enabled.
     */
    protected void enable() throws JposException {
        log.success("enable(in)", 1);
        if (!isConnected()) {
            JposException jposException = new JposException(JposConst.JPOS_E_OFFLINE);
            throw jposException;
        }
        deviceListener.startEventListeners();
        try {
            MSR msr;
            synchronized (msr = dynamicMSR.getDevice()) {
                msr.setAutoDisable(true);
                msr.setDataEventEnabled(true);
                msr.setDeviceEnabled(true);
            }
        } catch (JposException jposException) {
            if (isConnected()) {
                log.failure("Failed to Enable Device: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended(), 17, jposException);
            } else {
                log.failure("Failed to Enable Device: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended(), 17, jposException);
            }
            throw jposException;
        }
        log.success("MSR enabled", 9);
        log.success("enable(out)", 1);
    }

    /**
     * Disables MSR.
     */
    private void disable() throws JposException {
        log.success("disable(in)", 1);
        try {
            MSR msr;
            synchronized (msr = dynamicMSR.getDevice()) {
                msr.setDeviceEnabled(false);
            }
        } catch (JposException jposException) {
            if (isConnected()) {
                log.failure("Failed to Disable Device: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended(), 17, jposException);
            } else if (jposException.getErrorCode() != JposConst.JPOS_E_CLOSED) {
                log.failure("Failed to Disable Device: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended(), 17, jposException);
            }
            throw jposException;
        }
        log.success("MSR disabled", 9);
        log.success("disable(out)", 1);
    }

    /**
     * Attaches event listeners to the device.
     */
    private void attachEventListeners() {
        MSR msr;
        synchronized (msr = dynamicMSR.getDevice()) {
            msr.addErrorListener(deviceListener);
            msr.addDataListener(deviceListener);
            msr.addStatusUpdateListener(deviceListener);
        }
    }

    /**
     * Removes event listeners from the device.
     */
    private void detachEventListeners() {
        MSR msr;
        synchronized (msr = dynamicMSR.getDevice()) {
            msr.removeErrorListener(deviceListener);
            msr.removeDataListener(deviceListener);
            msr.removeStatusUpdateListener(deviceListener);
        }
    }

    /**
     * Lock the current resource.
     */
    public boolean tryLock() {
        try {
            isLocked = connectLock.tryLock(10, TimeUnit.SECONDS);
            log.success("Lock: " + isLocked, 1);
        } catch (InterruptedException interruptedException) {
            log.failure("Lock Failed: " + interruptedException.getMessage(), 17, interruptedException);
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
