package com.target.devicemanager.components.keylock;

import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.EventSynchronizer;
import jpos.JposConst;
import jpos.KeylockConst;
import org.springframework.context.annotation.Profile;

@Profile({"local", "dev", "prod"})
public class KeylockDeviceListener extends DeviceListener {

    public KeylockDeviceListener(EventSynchronizer eventSynchronizer) {
        super(eventSynchronizer);
    }

    @Override
    protected boolean isFailureStatus(int status) {
        switch (status) {
            case KeylockConst.LOCK_KP_LOCK:
            case KeylockConst.LOCK_KP_NORM:
            case KeylockConst.LOCK_KP_SUPR:
            case JposConst.JPOS_PS_ONLINE:
                return false;
            default:
                return true;
        }
    }
}
