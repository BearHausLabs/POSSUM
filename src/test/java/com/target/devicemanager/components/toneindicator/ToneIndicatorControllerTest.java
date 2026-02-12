package com.target.devicemanager.components.toneindicator;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.toneindicator.entities.ToneRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ToneIndicatorControllerTest {

    private ToneIndicatorController toneIndicatorController;

    @Mock
    private ToneIndicatorManager mockToneIndicatorManager;

    @BeforeEach
    public void testInitialize() {
        toneIndicatorController = new ToneIndicatorController(mockToneIndicatorManager);
    }

    @Test
    public void ctor_WhenToneIndicatorManagerIsNull_ThrowsException() {
        try {
            new ToneIndicatorController(null);
        } catch (IllegalArgumentException iae) {
            assertEquals("toneIndicatorManager cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenToneIndicatorManagerIsNew_DoesNotThrowException() {
        try {
            new ToneIndicatorController(mockToneIndicatorManager);
        } catch (Exception exception) {
            fail("Existing Manager Argument should not result in an Exception");
        }
    }

    @Test
    public void playSound_CallsThroughToToneIndicatorManager() throws DeviceException {
        //arrange
        ToneRequest toneRequest = new ToneRequest(1500, 100, 50);

        //act
        toneIndicatorController.playSound(toneRequest);

        //assert
        verify(mockToneIndicatorManager).playSound(toneRequest);
    }

    @Test
    public void playSound_WhenThrowsError() throws DeviceException {
        //arrange
        ToneRequest toneRequest = new ToneRequest(1500, 100, 50);
        doThrow(new DeviceException(DeviceError.DEVICE_BUSY)).when(mockToneIndicatorManager).playSound(toneRequest);

        //act
        try {
            toneIndicatorController.playSound(toneRequest);
        }

        //assert
        catch (DeviceException deviceException) {
            verify(mockToneIndicatorManager).playSound(toneRequest);
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void reconnect_CallsThroughToToneIndicatorManager() throws DeviceException {
        //arrange

        //act
        try {
            toneIndicatorController.reconnect();
        } catch (DeviceException deviceException) {
            fail("toneIndicatorController.reconnect() should not result in an Exception");
        }

        //assert
        verify(mockToneIndicatorManager).reconnectDevice();
    }

    @Test
    public void reconnect_WhenThrowsError() throws DeviceException {
        //arrange
        doThrow(new DeviceException(DeviceError.DEVICE_BUSY)).when(mockToneIndicatorManager).reconnectDevice();

        //act
        try {
            toneIndicatorController.reconnect();
        }

        //assert
        catch (DeviceException deviceException) {
            verify(mockToneIndicatorManager).reconnectDevice();
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void getHealth_ReturnsHealthFromManager() {
        //arrange
        DeviceHealthResponse expected = new DeviceHealthResponse("toneIndicator", DeviceHealth.READY);
        when(mockToneIndicatorManager.getHealth()).thenReturn(expected);

        //act
        DeviceHealthResponse actual = toneIndicatorController.getHealth();

        //assert
        assertEquals(expected, actual);
        verify(mockToneIndicatorManager).getHealth();
    }

    @Test
    public void getStatus_ReturnsStatusFromManager() {
        //arrange
        DeviceHealthResponse expected = new DeviceHealthResponse("toneIndicator", DeviceHealth.READY);
        when(mockToneIndicatorManager.getStatus()).thenReturn(expected);

        //act
        DeviceHealthResponse actual = toneIndicatorController.getStatus();

        //assert
        assertEquals(expected, actual);
        verify(mockToneIndicatorManager).getStatus();
    }
}
