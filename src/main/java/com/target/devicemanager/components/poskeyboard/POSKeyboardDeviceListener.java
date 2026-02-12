package com.target.devicemanager.components.poskeyboard;

import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.EventSynchronizer;
import jpos.JposConst;
import org.springframework.context.annotation.Profile;

@Profile({"local", "dev", "prod"})
public class POSKeyboardDeviceListener extends DeviceListener {

    public POSKeyboardDeviceListener(EventSynchronizer eventSynchronizer) {
        super(eventSynchronizer);
    }

    @Override
    protected boolean isFailureStatus(int status) {
        switch (status) {
            case JposConst.JPOS_SUE_POWER_ONLINE:
                return false;
            default:
                return true;
        }
    }
}
