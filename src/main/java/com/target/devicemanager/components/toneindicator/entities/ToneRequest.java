package com.target.devicemanager.components.toneindicator.entities;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request object for playing custom tones on ToneIndicator.
 * Contains parameters for frequency, duration, and repetition.
 */
public class ToneRequest {
    
    @Min(value = 100, message = "Frequency must be at least 100 Hz")
    @Max(value = 10000, message = "Frequency must not exceed 10000 Hz")
    private int frequency;  // Hz
    
    @Min(value = 50, message = "Duration must be at least 50 ms")
    @Max(value = 5000, message = "Duration must not exceed 5000 ms")
    private int duration;   // milliseconds
    
    @Min(value = 1, message = "Number of cycles must be at least 1")
    @Max(value = 10, message = "Number of cycles must not exceed 10")
    private int numberOfCycles;
    
    @Min(value = 0, message = "Inter-sound wait must be non-negative")
    private int interSoundWait; // milliseconds
    
    public ToneRequest() {
        // Default constructor for JSON deserialization
    }
    
    public ToneRequest(int frequency, int duration, int numberOfCycles, int interSoundWait) {
        this.frequency = frequency;
        this.duration = duration;
        this.numberOfCycles = numberOfCycles;
        this.interSoundWait = interSoundWait;
    }
    
    public int getFrequency() {
        return frequency;
    }
    
    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public int getNumberOfCycles() {
        return numberOfCycles;
    }
    
    public void setNumberOfCycles(int numberOfCycles) {
        this.numberOfCycles = numberOfCycles;
    }
    
    public int getInterSoundWait() {
        return interSoundWait;
    }
    
    public void setInterSoundWait(int interSoundWait) {
        this.interSoundWait = interSoundWait;
    }
    
    @Override
    public String toString() {
        return "ToneRequest{" +
                "frequency=" + frequency +
                ", duration=" + duration +
                ", numberOfCycles=" + numberOfCycles +
                ", interSoundWait=" + interSoundWait +
                '}';
    }
}

