package com.target.devicemanager.components.poskeyboard;

import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.components.poskeyboard.entities.KeyboardEventData;
import jpos.POSKeyboard;
import jpos.POSKeyboardConst;
import jpos.JposConst;
import jpos.JposException;
import jpos.events.DataEvent;
import jpos.events.DataListener;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Profile({"local", "dev", "prod"})
public class POSKeyboardDevice implements DataListener, StatusUpdateListener {
    private final DynamicDevice<? extends POSKeyboard> dynamicKeyboard;
    private final DeviceListener deviceListener;
    private boolean deviceConnected = false;
    private boolean areListenersAttached;
    private final ReentrantLock connectLock;
    private boolean isLocked = false;
    private Consumer<KeyboardEventData> eventCallback;
    private static final Logger LOGGER = LoggerFactory.getLogger(POSKeyboardDevice.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("POSKeyboard", "POSKeyboardDevice", LOGGER);

    public POSKeyboardDevice(DynamicDevice<? extends POSKeyboard> dynamicKeyboard, DeviceListener deviceListener) {
        this(dynamicKeyboard, deviceListener, new ReentrantLock(true));
    }

    public POSKeyboardDevice(DynamicDevice<? extends POSKeyboard> dynamicKeyboard, DeviceListener deviceListener, ReentrantLock connectLock) {
        if (dynamicKeyboard == null) {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException("dynamicKeyboard cannot be null");
            log.failure("POSKeyboard Failed in Constructor: dynamicKeyboard cannot be null", 18,
                    illegalArgumentException);
            throw illegalArgumentException;
        }
        if (deviceListener == null) {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException("deviceListener cannot be null");
            log.failure("POSKeyboard Failed in Constructor: deviceListener cannot be null", 18,
                    illegalArgumentException);
            throw illegalArgumentException;
        }
        this.dynamicKeyboard = dynamicKeyboard;
        this.deviceListener = deviceListener;
        this.connectLock = connectLock;
    }

    /**
     * Connects to the POS keyboard device, attaches listeners, and enables it.
     */
    public boolean connect() {
        if (dynamicKeyboard.connect() == DynamicDevice.ConnectionResult.NOT_CONNECTED) {
            return false;
        }
        if (!areListenersAttached) {
            attachEventListeners();
            areListenersAttached = true;
        }

        POSKeyboard keyboard;
        synchronized (keyboard = dynamicKeyboard.getDevice()) {
            try {
                if (!keyboard.getDeviceEnabled()) {
                    // Must set event types BEFORE enabling - tells the device to deliver key events
                    try {
                        keyboard.setEventTypes(POSKeyboardConst.KBD_ET_DOWN_UP);
                        log.success("setEventTypes(KBD_ET_DOWN_UP)", 5);
                    } catch (JposException jposException) {
                        log.failure("Failed to set event types", 17, jposException);
                    }
                    keyboard.setDeviceEnabled(true);
                    try {
                        keyboard.setDataEventEnabled(true);
                    } catch (JposException jposException) {
                        log.failure("Failed to enable data events", 17, jposException);
                    }
                    deviceConnected = true;
                }
            } catch (JposException jposException) {
                deviceConnected = false;
                return false;
            }
        }
        return true;
    }

    /**
     * This method is only used to set 'areListenersAttached' for unit testing
     */
    public void setAreListenersAttached(boolean areListenersAttached) {
        this.areListenersAttached = areListenersAttached;
    }

    /**
     * This method is only used to get 'areListenersAttached' for unit testing
     */
    public boolean getAreListenersAttached() {
        return areListenersAttached;
    }

    /**
     * This method is only used to set 'deviceConnected' for unit testing
     */
    public void setDeviceConnected(boolean deviceConnected) {
        this.deviceConnected = deviceConnected;
    }

    /**
     * Disconnects the POS keyboard device.
     */
    public void disconnect() {
        if (dynamicKeyboard.isConnected()) {
            if (areListenersAttached) {
                detachEventListeners();
                areListenersAttached = false;
            }
            POSKeyboard keyboard;
            synchronized (keyboard = dynamicKeyboard.getDevice()) {
                try {
                    if (keyboard.getDeviceEnabled()) {
                        keyboard.setDataEventEnabled(false);
                        keyboard.setDeviceEnabled(false);
                        dynamicKeyboard.disconnect();
                        deviceConnected = false;
                    }
                } catch (JposException jposException) {
                    log.failure("POSKeyboard Failed to Disconnect", 18, jposException);
                }
            }
        }
        /*
        Re-enable device when not connected to get status update events.
        */
        POSKeyboard keyboard;
        synchronized (keyboard = dynamicKeyboard.getDevice()) {
            try {
                if (!keyboard.getDeviceEnabled()) {
                    try {
                        keyboard.setEventTypes(POSKeyboardConst.KBD_ET_DOWN_UP);
                    } catch (JposException jposException) {
                        log.failure("Failed to set event types on re-enable", 17, jposException);
                    }
                    keyboard.setDeviceEnabled(true);
                    try {
                        keyboard.setDataEventEnabled(true);
                    } catch (JposException jposException) {
                        log.failure("Failed to re-enable data events after disconnect", 17, jposException);
                    }
                    deviceConnected = true;
                }
            } catch (JposException jposException) {
                log.failure("POSKeyboard Failed to Enable Device", 18, jposException);
                deviceConnected = false;
            }
        }
    }

    /**
     * Gets the device name.
     */
    public String getDeviceName() {
        return dynamicKeyboard.getDeviceName();
    }

    /**
     * Shows if the device is connected.
     */
    public boolean isConnected() {
        return deviceConnected;
    }

    /**
     * Sets the callback for keyboard events. The manager registers itself here.
     */
    public void setEventCallback(Consumer<KeyboardEventData> callback) {
        this.eventCallback = callback;
    }

    /**
     * Attaches both data and status update listeners to the device.
     */
    private void attachEventListeners() {
        POSKeyboard keyboard;
        synchronized (keyboard = dynamicKeyboard.getDevice()) {
            keyboard.addDataListener(this);
            keyboard.addStatusUpdateListener(this);
        }
    }

    /**
     * Removes data and status update listeners from the device.
     */
    private void detachEventListeners() {
        POSKeyboard keyboard;
        synchronized (keyboard = dynamicKeyboard.getDevice()) {
            keyboard.removeDataListener(this);
            keyboard.removeStatusUpdateListener(this);
        }
    }

    /**
     * JavaPOS callback fired when a key is pressed or released.
     * Captures the key data, creates a KeyboardEventData, notifies the manager
     * via the callback, and re-enables data events for the next key press.
     */
    @Override
    public void dataOccurred(DataEvent dataEvent) {
        log.success("POSKeyboard dataOccurred(): " + dataEvent.getStatus(), 1);
        try {
            POSKeyboard keyboard;
            synchronized (keyboard = dynamicKeyboard.getDevice()) {
                int keyData = keyboard.getPOSKeyData();
                int eventType = keyboard.getPOSKeyEventType();

                String eventTypeName = (eventType == POSKeyboardConst.KBD_KET_KEYDOWN)
                        ? "KEY_DOWN" : "KEY_UP";

                KeyboardEventData event = new KeyboardEventData(
                        keyData,
                        eventTypeName,
                        System.currentTimeMillis()
                );

                log.success("Key event: code=" + keyData + " type=" + eventTypeName, 1);

                if (eventCallback != null) {
                    eventCallback.accept(event);
                }

                keyboard.setDataEventEnabled(true);
            }
        } catch (JposException jposException) {
            log.failure("Error processing keyboard data event", 17, jposException);
            try {
                POSKeyboard keyboard;
                synchronized (keyboard = dynamicKeyboard.getDevice()) {
                    keyboard.setDataEventEnabled(true);
                }
            } catch (JposException e) {
                log.failure("Failed to re-enable data events after error", 17, e);
            }
        }
    }

    /**
     * JavaPOS callback for device status changes like power and connectivity.
     */
    @Override
    public void statusUpdateOccurred(StatusUpdateEvent statusUpdateEvent) {
        int status = statusUpdateEvent.getStatus();
        log.success("POSKeyboard statusUpdateOccurred(): " + status, 1);
        switch (status) {
            case JposConst.JPOS_SUE_POWER_OFF:
            case JposConst.JPOS_SUE_POWER_OFF_OFFLINE:
            case JposConst.JPOS_SUE_POWER_OFFLINE:
                log.failure("POSKeyboard Status Update: Power offline", 13, null);
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

    /**
     * Lock the current resource.
     */
    public boolean tryLock() {
        try {
            isLocked = connectLock.tryLock(10, TimeUnit.SECONDS);
            log.success("Lock: " + isLocked, 1);
        } catch (InterruptedException interruptedException) {
            log.failure("Lock Failed", 17, interruptedException);
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

    /**
     * This method is only used to get "isLocked" for unit testing
     */
    public boolean getIsLocked() {
        return isLocked;
    }
}
