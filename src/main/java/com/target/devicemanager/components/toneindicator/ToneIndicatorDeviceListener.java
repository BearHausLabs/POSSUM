package com.target.devicemanager.components.toneindicator;

import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.EventSynchronizer;
import jpos.JposConst;
import org.springframework.context.annotation.Profile;

@Profile({"local", "dev", "prod"})
public class ToneIndicatorDeviceListener extends DeviceListener {

    public ToneIndicatorDeviceListener(EventSynchronizer eventSynchronizer) {
        super(eventSynchronizer);
    }

    @Override
    protected boolean isFailureStatus(int status) {
        switch (status) {
            case JposConst.JPOS_PS_ONLINE:
                return false;
            default:
                return true;
        }
    }
}
