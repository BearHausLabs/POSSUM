package com.target.devicemanager.components.keylock.entities;

import com.target.devicemanager.common.entities.DeviceError;
import org.springframework.http.HttpStatus;

public class KeylockError extends DeviceError {
    public static final DeviceError POSITION_READ_FAILED = new KeylockError("POSITION_READ_FAILED", "Failed to read key position", HttpStatus.PRECONDITION_FAILED);

    public KeylockError(String code, String description, HttpStatus statusCode) {
        super(code, description, statusCode);
    }
}
