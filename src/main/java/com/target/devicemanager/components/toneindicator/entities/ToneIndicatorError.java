package com.target.devicemanager.components.toneindicator.entities;

import com.target.devicemanager.common.entities.DeviceError;
import org.springframework.http.HttpStatus;

public class ToneIndicatorError extends DeviceError {
    public static final DeviceError SOUND_FAILED = new ToneIndicatorError("SOUND_FAILED", "Failed to play tone", HttpStatus.PRECONDITION_FAILED);

    public ToneIndicatorError(String code, String description, HttpStatus statusCode) {
        super(code, description, statusCode);
    }
}
