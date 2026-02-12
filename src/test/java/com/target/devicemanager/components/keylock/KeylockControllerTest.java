package com.target.devicemanager.components.keylock;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.keylock.entities.KeylockPosition;
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
public class KeylockControllerTest {

    private KeylockController keylockController;

    @Mock
    private KeylockManager mockKeylockManager;

    @BeforeEach
    public void testInitialize() {
        keylockController = new KeylockController(mockKeylockManager);
    }

    @Test
    public void ctor_WhenKeylockManagerIsNull_ThrowsException() {
        try {
            new KeylockController(null);
        } catch (IllegalArgumentException iae) {
            assertEquals("keylockManager cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenKeylockManagerIsNew_DoesNotThrowException() {
        try {
            new KeylockController(mockKeylockManager);
        } catch (Exception exception) {
            fail("Existing Manager Argument should not result in an Exception");
        }
    }

    @Test
    public void getKeyPosition_CallsThroughToKeylockManager() throws DeviceException {
        //arrange
        when(mockKeylockManager.getKeyPosition()).thenReturn(KeylockPosition.NORMAL);

        //act
        KeylockPosition position = keylockController.getKeyPosition();

        //assert
        assertEquals(KeylockPosition.NORMAL, position);
        verify(mockKeylockManager).getKeyPosition();
    }

    @Test
    public void getKeyPosition_WhenThrowsError() throws DeviceException {
        //arrange
        doThrow(new DeviceException(DeviceError.DEVICE_BUSY)).when(mockKeylockManager).getKeyPosition();

        //act
        try {
            keylockController.getKeyPosition();
        }

        //assert
        catch(DeviceException deviceException) {
            verify(mockKeylockManager).getKeyPosition();
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void getKeyPosition_WhenDeviceOffline_ThrowsError() throws DeviceException {
        //arrange
        doThrow(new DeviceException(DeviceError.DEVICE_OFFLINE)).when(mockKeylockManager).getKeyPosition();

        //act
        try {
            keylockController.getKeyPosition();
        }

        //assert
        catch(DeviceException deviceException) {
            verify(mockKeylockManager).getKeyPosition();
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void reconnect_CallsThroughToKeylockManager() throws DeviceException {
        //arrange

        //act
        try {
            keylockController.reconnect();
        } catch (DeviceException deviceException) {
            fail("keylockController.reconnect() should not result in an Exception");
        }

        //assert
        verify(mockKeylockManager).reconnectDevice();
    }

    @Test
    public void reconnect_WhenThrowsError() throws DeviceException {
        //arrange
        doThrow(new DeviceException(DeviceError.DEVICE_BUSY)).when(mockKeylockManager).reconnectDevice();

        //act
        try {
            keylockController.reconnect();
        }

        //assert
        catch(DeviceException deviceException) {
            verify(mockKeylockManager).reconnectDevice();
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void getHealth_ReturnsHealthFromManager() {
        //arrange
        DeviceHealthResponse expected = new DeviceHealthResponse("keylock", DeviceHealth.READY);
        when(mockKeylockManager.getHealth()).thenReturn(expected);

        //act
        DeviceHealthResponse actual = keylockController.getHealth();

        //assert
        assertEquals(expected, actual);
        verify(mockKeylockManager).getHealth();
    }

    @Test
    public void getStatus_ReturnsStatusFromManager() {
        //arrange
        DeviceHealthResponse expected = new DeviceHealthResponse("keylock", DeviceHealth.READY);
        when(mockKeylockManager.getStatus()).thenReturn(expected);

        //act
        DeviceHealthResponse actual = keylockController.getStatus();

        //assert
        assertEquals(expected, actual);
        verify(mockKeylockManager).getStatus();
    }
}
