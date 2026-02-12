package com.target.devicemanager.components.msr;

import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.EventSynchronizer;
import jpos.JposConst;

public class MSRDeviceListener extends DeviceListener {

    public MSRDeviceListener(EventSynchronizer eventSynchronizer) {
        super(eventSynchronizer);
    }

    @Override
    protected boolean isFailureStatus(int status) {
        switch (status) {
            case JposConst.JPOS_PS_UNKNOWN:
            case JposConst.JPOS_SUE_POWER_ONLINE:
                return false;
            default:
                return true;
        }
    }
}
