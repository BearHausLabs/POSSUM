package com.target.devicemanager.components.toneindicator.simulator;

import java.util.Vector;

import com.target.devicemanager.common.SimulatorState;
import jpos.JposConst;
import jpos.ToneIndicator;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;
import org.springframework.context.annotation.Profile;

@Profile("local")
public class SimulatedJposToneIndicator extends ToneIndicator {
    private SimulatorState simulatorState;
    private int statusUpdateStatus = JposConst.JPOS_SUE_POWER_OFF_OFFLINE;

    // Store tone parameters for verification
    private int lastTone1Pitch;
    private int lastTone1Duration;
    private int lastTone1Volume;
    private int lastTone2Pitch;
    private int lastTone2Duration;
    private int lastTone2Volume;
    private int lastInterToneWait;

    public SimulatedJposToneIndicator() {
        simulatorState = SimulatorState.ONLINE;
    }

    void setState(SimulatorState simulatorState) {
        statusUpdateStatus = simulatorState.getStatus();
        this.simulatorState = simulatorState;
        triggerStatusUpdateEvent();
    }

    @Override
    public void setTone1Pitch(int pitch) {
        this.lastTone1Pitch = pitch;
    }

    @Override
    public void setTone1Duration(int duration) {
        this.lastTone1Duration = duration;
    }

    @Override
    public void setTone1Volume(int volume) {
        this.lastTone1Volume = volume;
    }

    @Override
    public void setTone2Pitch(int pitch) {
        this.lastTone2Pitch = pitch;
    }

    @Override
    public void setTone2Duration(int duration) {
        this.lastTone2Duration = duration;
    }

    @Override
    public void setTone2Volume(int volume) {
        this.lastTone2Volume = volume;
    }

    @Override
    public void setInterToneWait(int wait) {
        this.lastInterToneWait = wait;
    }

    @Override
    public void sound(int numberOfCycles, int interSoundWait) {
        // Simulate playing a tone - just trigger a status update event
        triggerStatusUpdateEvent();
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

    private void triggerStatusUpdateEvent() {
        StatusUpdateEvent statusUpdateEvent = new StatusUpdateEvent(this, statusUpdateStatus);

        ((Vector<StatusUpdateListener>) statusUpdateListeners).forEach(listener ->
                listener.statusUpdateOccurred(statusUpdateEvent)
        );
    }
}
