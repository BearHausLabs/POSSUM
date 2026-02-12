package com.target.devicemanager.components.toneindicator.entities;

public class ToneRequest {
    public int pitch1;
    public int duration1;
    public int volume1;
    public int pitch2;
    public int duration2;
    public int volume2;
    public int interToneWait;

    public ToneRequest() {
    }

    public ToneRequest(int pitch1, int duration1, int volume1) {
        this.pitch1 = pitch1;
        this.duration1 = duration1;
        this.volume1 = volume1;
    }

    public ToneRequest(int pitch1, int duration1, int volume1,
                       int pitch2, int duration2, int volume2,
                       int interToneWait) {
        this.pitch1 = pitch1;
        this.duration1 = duration1;
        this.volume1 = volume1;
        this.pitch2 = pitch2;
        this.duration2 = duration2;
        this.volume2 = volume2;
        this.interToneWait = interToneWait;
    }

    @Override
    public String toString() {
        return "ToneRequest{" +
                "pitch1=" + pitch1 +
                ", duration1=" + duration1 +
                ", volume1=" + volume1 +
                ", pitch2=" + pitch2 +
                ", duration2=" + duration2 +
                ", volume2=" + volume2 +
                ", interToneWait=" + interToneWait +
                '}';
    }
}
