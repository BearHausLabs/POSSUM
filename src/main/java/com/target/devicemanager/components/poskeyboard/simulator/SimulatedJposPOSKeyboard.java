package com.target.devicemanager.components.poskeyboard.simulator;

import java.util.Vector;

import com.target.devicemanager.common.SimulatorState;
import jpos.POSKeyboard;
import jpos.POSKeyboardConst;
import jpos.JposConst;
import jpos.events.DataEvent;
import jpos.events.DataListener;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;
import org.springframework.context.annotation.Profile;

@Profile("local")
public class SimulatedJposPOSKeyboard extends POSKeyboard {
    private SimulatorState simulatorState;
    private int statusUpdateStatus = JposConst.JPOS_SUE_POWER_OFF_OFFLINE;
    private int lastKeyData = 0;
    private int lastKeyEventType = POSKeyboardConst.KBD_KET_KEYDOWN;

    public SimulatedJposPOSKeyboard() {
        simulatorState = SimulatorState.ONLINE;
    }

    void setState(SimulatorState simulatorState) {
        statusUpdateStatus = simulatorState.getStatus();
        this.simulatorState = simulatorState;
        triggerStatusUpdateEvent();
    }

    void simulateKeyPress(int keyCode) {
        this.lastKeyData = keyCode;
        this.lastKeyEventType = POSKeyboardConst.KBD_KET_KEYDOWN;
        triggerDataEvent();
    }

    private void triggerDataEvent() {
        DataEvent dataEvent = new DataEvent(this, 0);

        for (Object object : dataListeners) {
            DataListener dataListener = (DataListener) object;
            dataListener.dataOccurred(dataEvent);
        }
    }

    private void triggerStatusUpdateEvent() {
        StatusUpdateEvent statusUpdateEvent = new StatusUpdateEvent(this, statusUpdateStatus);

        ((Vector<StatusUpdateListener>) statusUpdateListeners).forEach(listener ->
                listener.statusUpdateOccurred(statusUpdateEvent)
        );
    }

    @Override
    public int getPOSKeyData() {
        return lastKeyData;
    }

    @Override
    public int getPOSKeyEventType() {
        return lastKeyEventType;
    }

    @Override
    public boolean getCapKeyUp() {
        return true;
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
        // doNothing
    }

    @Override
    public void setDataEventEnabled(boolean value) {
        // doNothing
    }
}
