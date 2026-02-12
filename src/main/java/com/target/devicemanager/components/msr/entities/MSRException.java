package com.target.devicemanager.components.msr.entities;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import jpos.JposConst;
import jpos.JposException;

public class MSRException extends DeviceException {
    private static final long serialVersionUID = 1L;

    public MSRException(DeviceError msrError) {
        super(msrError);
    }

    public MSRException(JposException originalException) {
        causedBy = originalException;

        errorCodeMap.put(JposConst.JPOS_E_DISABLED, MSRError.DISABLED);
        errorCodeMap.put(JposConst.JPOS_E_TIMEOUT, MSRError.DISABLED);
        errorCodeMap.put(JposConst.JPOS_E_FAILURE, MSRError.UNEXPECTED_ERROR);
        errorCodeMap.put(JposConst.JPOS_E_ILLEGAL, MSRError.UNEXPECTED_ERROR);
        errorCodeMap.put(JposConst.JPOS_E_CLOSED, MSRError.DEVICE_OFFLINE);

        super.deviceError = errorCodeMap.getOrDefault(originalException.getErrorCode(),
                errorCodeMap.getOrDefault(originalException.getErrorCodeExtended(), DeviceError.UNEXPECTED_ERROR));
    }
}
