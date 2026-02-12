package com.target.devicemanager.components.poskeyboard.entities;

import com.target.devicemanager.common.entities.DeviceError;
import org.springframework.http.HttpStatus;

public class POSKeyboardError extends DeviceError {
    public static final DeviceError EVENT_READ_FAILED = new POSKeyboardError("EVENT_READ_FAILED", "Failed to read keyboard event data", HttpStatus.PRECONDITION_FAILED);

    public POSKeyboardError(String code, String description, HttpStatus statusCode) {
        super(code, description, statusCode);
    }
}
