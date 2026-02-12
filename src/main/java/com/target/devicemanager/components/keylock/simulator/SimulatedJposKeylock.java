package com.target.devicemanager.components.keylock.simulator;

import java.util.Vector;

import com.target.devicemanager.common.SimulatorState;
import jpos.Keylock;
import jpos.KeylockConst;
import jpos.JposConst;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;
import org.springframework.context.annotation.Profile;

@Profile("local")
public class SimulatedJposKeylock extends Keylock {
    private KeylockPositionStatus keylockPositionStatus;
    private SimulatorState simulatorState;
    private int statusUpdateStatus = JposConst.JPOS_SUE_POWER_OFF_OFFLINE;
    private int keyPosition = KeylockConst.LOCK_KP_LOCK;

    public SimulatedJposKeylock() {
        simulatorState = SimulatorState.ONLINE;
        this.keylockPositionStatus = KeylockPositionStatus.LOCKED;
    }

    void setState(SimulatorState simulatorState) {
        statusUpdateStatus = simulatorState.getStatus();
        this.simulatorState = simulatorState;

        triggerStatusUpdateEvent();
    }

    void setPosition(KeylockPositionStatus keylockPositionStatus) {
        this.keylockPositionStatus = keylockPositionStatus;
        switch (keylockPositionStatus) {
            case LOCKED:
                keyPosition = KeylockConst.LOCK_KP_LOCK;
                break;
            case NORMAL:
                keyPosition = KeylockConst.LOCK_KP_NORM;
                break;
            case SUPERVISOR:
                keyPosition = KeylockConst.LOCK_KP_SUPR;
                break;
        }
        triggerKeyPositionEvent();
    }

    private void triggerKeyPositionEvent() {
        StatusUpdateEvent statusUpdateEvent = new StatusUpdateEvent(this, keyPosition);

        for (Object object : statusUpdateListeners) {
            StatusUpdateListener statusUpdateListener = (StatusUpdateListener) object;
            statusUpdateListener.statusUpdateOccurred(statusUpdateEvent);
        }
    }

    @Override
    public int getKeyPosition() {
        return keyPosition;
    }

    @Override
    public int getState() {
        return simulatorState == SimulatorState.ONLINE ? JposConst.JPOS_S_IDLE : JposConst.JPOS_S_CLOSED;
    }

    @Override
    public boolean getDeviceEnabled() {
        return false;
    }

    @Override
    public void setDeviceEnabled(boolean value) {
        //doNothing
    }

    private void triggerStatusUpdateEvent() {
        StatusUpdateEvent statusUpdateEvent = new StatusUpdateEvent(this, statusUpdateStatus);

        ((Vector<StatusUpdateListener>) statusUpdateListeners).forEach(listener ->
                listener.statusUpdateOccurred(statusUpdateEvent)
        );
    }
}
