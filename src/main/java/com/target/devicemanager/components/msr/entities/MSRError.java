package com.target.devicemanager.components.msr.entities;

import com.target.devicemanager.common.entities.DeviceError;
import org.springframework.http.HttpStatus;

public class MSRError extends DeviceError {
    public static final MSRError DISABLED = new MSRError("DISABLED", "Card read request canceled", HttpStatus.NO_CONTENT);
    public static final MSRError ALREADY_DISABLED = new MSRError("ALREADY_DISABLED", "Card read not in progress. Nothing to cancel.", HttpStatus.PRECONDITION_FAILED);

    public MSRError(String code, String description, HttpStatus statusCode) {
        super(code, description, statusCode);
    }
}
