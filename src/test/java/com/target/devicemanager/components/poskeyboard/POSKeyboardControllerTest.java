package com.target.devicemanager.components.poskeyboard;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class POSKeyboardControllerTest {

    private POSKeyboardController posKeyboardController;

    @Mock
    private POSKeyboardManager mockPosKeyboardManager;

    @BeforeEach
    public void testInitialize() {
        posKeyboardController = new POSKeyboardController(mockPosKeyboardManager);
    }

    @Test
    public void ctor_WhenManagerIsNull_ThrowsException() {
        try {
            new POSKeyboardController(null);
        } catch (IllegalArgumentException iae) {
            assertEquals("posKeyboardManager cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenManagerIsNew_DoesNotThrowException() {
        try {
            new POSKeyboardController(mockPosKeyboardManager);
        } catch (Exception exception) {
            fail("Existing Manager Argument should not result in an Exception");
        }
    }

    @Test
    public void subscribeToKeyboardEvents_ReturnsSseEmitter() {
        //act
        SseEmitter emitter = posKeyboardController.subscribeToKeyboardEvents();

        //assert
        assertNotNull(emitter);
        verify(mockPosKeyboardManager).addEventSubscriber(any(SseEmitter.class));
    }

    @Test
    public void reconnect_CallsThroughToManager() throws DeviceException {
        //act
        try {
            posKeyboardController.reconnect();
        } catch (DeviceException deviceException) {
            fail("reconnect should not result in an Exception");
        }

        //assert
        verify(mockPosKeyboardManager).reconnectDevice();
    }

    @Test
    public void reconnect_WhenThrowsError() throws DeviceException {
        //arrange
        doThrow(new DeviceException(DeviceError.DEVICE_BUSY)).when(mockPosKeyboardManager).reconnectDevice();

        //act
        try {
            posKeyboardController.reconnect();
        }

        //assert
        catch (DeviceException deviceException) {
            verify(mockPosKeyboardManager).reconnectDevice();
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void reconnect_WhenDeviceOffline_ThrowsError() throws DeviceException {
        //arrange
        doThrow(new DeviceException(DeviceError.DEVICE_OFFLINE)).when(mockPosKeyboardManager).reconnectDevice();

        //act
        try {
            posKeyboardController.reconnect();
        }

        //assert
        catch (DeviceException deviceException) {
            verify(mockPosKeyboardManager).reconnectDevice();
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void getHealth_ReturnsHealthFromManager() {
        //arrange
        DeviceHealthResponse expected = new DeviceHealthResponse("POSKeyboard", DeviceHealth.READY);
        when(mockPosKeyboardManager.getHealth()).thenReturn(expected);

        //act
        DeviceHealthResponse actual = posKeyboardController.getHealth();

        //assert
        assertEquals(expected, actual);
        verify(mockPosKeyboardManager).getHealth();
    }

    @Test
    public void getStatus_ReturnsStatusFromManager() {
        //arrange
        DeviceHealthResponse expected = new DeviceHealthResponse("POSKeyboard", DeviceHealth.READY);
        when(mockPosKeyboardManager.getStatus()).thenReturn(expected);

        //act
        DeviceHealthResponse actual = posKeyboardController.getStatus();

        //assert
        assertEquals(expected, actual);
        verify(mockPosKeyboardManager).getStatus();
    }
}
