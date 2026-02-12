package com.target.devicemanager.components.msr;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.msr.entities.CardData;
import com.target.devicemanager.components.msr.entities.MSRException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MSRControllerTest {

    private MSRController msrController;

    @Mock
    private MSRManager mockMSRManager;

    @BeforeEach
    public void testInitialize() {
        msrController = new MSRController(mockMSRManager);
    }

    @Test
    public void ctor_WhenMSRManagerIsNull_ThrowsException() {
        try {
            new MSRController(null);
        } catch (IllegalArgumentException iae) {
            assertEquals("msrManager cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenMSRManagerIsNotNull_DoesNotThrowException() {
        try {
            new MSRController(mockMSRManager);
        } catch (Exception exception) {
            fail("Existing Manager Argument should not result in an Exception");
        }
    }

    @Test
    public void getCardData_WhenSuccess_ReturnsCardData() throws MSRException {
        //arrange
        CardData expected = new CardData("T1", "T2", "T3", "T4");
        when(mockMSRManager.getData()).thenReturn(expected);

        //act
        CardData actual = msrController.getCardData();

        //assert
        assertEquals(expected, actual);
        verify(mockMSRManager).getData();
    }

    @Test
    public void getCardData_WhenThrowsError() throws MSRException {
        //arrange
        doThrow(new MSRException(DeviceError.DEVICE_BUSY)).when(mockMSRManager).getData();

        //act
        try {
            msrController.getCardData();
        }

        //assert
        catch (MSRException msrException) {
            verify(mockMSRManager).getData();
            assertEquals(DeviceError.DEVICE_BUSY, msrException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void cancelReadRequest_CallsThroughToManager() throws MSRException {
        //arrange

        //act
        msrController.cancelReadRequest();

        //assert
        verify(mockMSRManager).cancelReadRequest();
    }

    @Test
    public void cancelReadRequest_WhenThrowsError() throws MSRException {
        //arrange
        doThrow(new MSRException(DeviceError.DEVICE_BUSY)).when(mockMSRManager).cancelReadRequest();

        //act
        try {
            msrController.cancelReadRequest();
        }

        //assert
        catch (MSRException msrException) {
            verify(mockMSRManager).cancelReadRequest();
            assertEquals(DeviceError.DEVICE_BUSY, msrException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void getHealth_ReturnsHealthFromManager() {
        //arrange
        DeviceHealthResponse expected = new DeviceHealthResponse("MSR", DeviceHealth.READY);
        ResponseEntity<DeviceHealthResponse> expectedResponse = ResponseEntity.ok(expected);
        when(mockMSRManager.getHealth()).thenReturn(expected);

        //act
        ResponseEntity<DeviceHealthResponse> actual = msrController.getHealth();

        //assert
        assertEquals(expectedResponse, actual);
        verify(mockMSRManager).getHealth();
    }

    @Test
    public void getStatus_ReturnsStatusFromManager() {
        //arrange
        DeviceHealthResponse expected = new DeviceHealthResponse("MSR", DeviceHealth.READY);
        ResponseEntity<DeviceHealthResponse> expectedResponse = ResponseEntity.ok(expected);
        when(mockMSRManager.getStatus()).thenReturn(expected);

        //act
        ResponseEntity<DeviceHealthResponse> actual = msrController.getStatus();

        //assert
        assertEquals(expectedResponse, actual);
        verify(mockMSRManager).getStatus();
    }

    @Test
    public void reconnect_CallsThroughToManager() throws DeviceException {
        //arrange

        //act
        try {
            msrController.reconnect();
        } catch (Exception exception) {
            fail("msrController.reconnect() should not result in an Exception");
        }

        //assert
        verify(mockMSRManager).reconnectDevice();
    }

    @Test
    public void reconnect_WhenThrowsError() throws DeviceException {
        //arrange
        doThrow(new DeviceException(DeviceError.DEVICE_BUSY)).when(mockMSRManager).reconnectDevice();

        //act
        try {
            msrController.reconnect();
        }

        //assert
        catch (DeviceException deviceException) {
            verify(mockMSRManager).reconnectDevice();
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }
}
