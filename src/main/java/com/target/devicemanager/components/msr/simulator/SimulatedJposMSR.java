package com.target.devicemanager.components.msr.simulator;

import com.target.devicemanager.common.SimulatorState;
import com.target.devicemanager.components.msr.entities.CardData;
import jpos.JposConst;
import jpos.MSR;
import jpos.events.DataEvent;
import jpos.events.DataListener;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;

import java.nio.charset.Charset;

public class SimulatedJposMSR extends MSR {
    private CardData cardData;
    private SimulatorState simulatorState;

    public SimulatedJposMSR() {
        cardData = new CardData(
                "POST desired data to MSR simulator",
                "POST desired data to MSR simulator",
                "",
                ""
        );
        simulatorState = SimulatorState.ONLINE;
    }

    void setCardData(CardData cardData) {
        this.cardData = cardData;
        triggerDataEvent();
    }

    void setState(SimulatorState simulatorState) {
        this.simulatorState = simulatorState;
        triggerStatusUpdateEvent();
    }

    private void triggerDataEvent() {
        DataEvent dataEvent = new DataEvent(this, JposConst.JPOS_SUCCESS);

        for (Object object : dataListeners) {
            DataListener dataListener = (DataListener) object;
            dataListener.dataOccurred(dataEvent);
        }
    }

    private void triggerStatusUpdateEvent() {
        int status = JposConst.JPOS_SUE_POWER_OFF_OFFLINE;

        if (simulatorState == SimulatorState.ONLINE) {
            status = JposConst.JPOS_SUE_POWER_ONLINE;
        }

        StatusUpdateEvent statusUpdateEvent = new StatusUpdateEvent(this, status);

        for (Object object : statusUpdateListeners) {
            StatusUpdateListener statusUpdateListener = (StatusUpdateListener) object;
            statusUpdateListener.statusUpdateOccurred(statusUpdateEvent);
        }
    }

    @Override
    public byte[] getTrack1Data() {
        return cardData.track1Data != null ? cardData.track1Data.getBytes(Charset.defaultCharset()) : new byte[0];
    }

    @Override
    public byte[] getTrack2Data() {
        return cardData.track2Data != null ? cardData.track2Data.getBytes(Charset.defaultCharset()) : new byte[0];
    }

    @Override
    public byte[] getTrack3Data() {
        return cardData.track3Data != null ? cardData.track3Data.getBytes(Charset.defaultCharset()) : new byte[0];
    }

    @Override
    public byte[] getTrack4Data() {
        return cardData.track4Data != null ? cardData.track4Data.getBytes(Charset.defaultCharset()) : new byte[0];
    }

    @Override
    public int getState() {
        return simulatorState == SimulatorState.ONLINE ? JposConst.JPOS_S_IDLE : JposConst.JPOS_S_CLOSED;
    }

    @Override
    public void setAutoDisable(boolean value) {
        //doNothing
    }

    @Override
    public void setDataEventEnabled(boolean value) {
        //doNothing
    }

    @Override
    public void setDeviceEnabled(boolean value) {
        //doNothing
    }

    @Override
    public void close() {
        //do nothing
    }
}
