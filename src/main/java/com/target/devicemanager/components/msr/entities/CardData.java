package com.target.devicemanager.components.msr.entities;

public class CardData {
    public String track1Data;
    public String track2Data;
    public String track3Data;
    public String track4Data;

    public CardData() {
    }

    public CardData(String track1Data, String track2Data, String track3Data, String track4Data) {
        this.track1Data = track1Data;
        this.track2Data = track2Data;
        this.track3Data = track3Data;
        this.track4Data = track4Data;
    }

    @Override
    public String toString() {
        return "CardData{" +
                "hasTrack1=" + (track1Data != null && !track1Data.isEmpty()) +
                ", hasTrack2=" + (track2Data != null && !track2Data.isEmpty()) +
                ", hasTrack3=" + (track3Data != null && !track3Data.isEmpty()) +
                ", hasTrack4=" + (track4Data != null && !track4Data.isEmpty()) +
                '}';
    }
}
