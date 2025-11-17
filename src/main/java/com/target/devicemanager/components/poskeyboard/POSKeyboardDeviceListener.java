package com.target.devicemanager.components.poskeyboard;

import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.EventSynchronizer;
import jpos.JposConst;
import jpos.events.DataEvent;

/**
 * Device listener for POSKeyboard events.
 * Extends the common DeviceListener to handle POSKeyboard-specific events.
 */
public class POSKeyboardDeviceListener extends DeviceListener {
    
    private POSKeyboardDevice posKeyboardDevice;
    
    public POSKeyboardDeviceListener(EventSynchronizer eventSynchronizer) {
        super(eventSynchronizer);
    }
    
    /**
     * Sets the POSKeyboardDevice to receive data events.
     * 
     * @param device the POSKeyboard device
     */
    public void setPOSKeyboardDevice(POSKeyboardDevice device) {
        this.posKeyboardDevice = device;
    }
    
    @Override
    public void dataOccurred(DataEvent dataEvent) {
        if (posKeyboardDevice != null) {
            posKeyboardDevice.handleDataEvent(dataEvent);
        }
        super.dataOccurred(dataEvent);
    }
    
    @Override
    protected boolean isFailureStatus(int status) {
        switch (status) {
            case JposConst.JPOS_PS_UNKNOWN:
            case JposConst.JPOS_PS_ONLINE:
                return false;
            default:
                return true;
        }
    }
}

