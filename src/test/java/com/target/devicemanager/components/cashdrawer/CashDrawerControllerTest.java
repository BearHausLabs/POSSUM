package com.target.devicemanager.components.cashdrawer;

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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CashDrawerControllerTest {

    private static final int DRAWER_ID = 1;

    private CashDrawerController cashDrawerController;

    @Mock
    private CashDrawerManager mockCashDrawerManager;

    @BeforeEach
    public void testInitialize() {
        cashDrawerController = new CashDrawerController(mockCashDrawerManager);
    }

    @Test
    public void ctor_WhenCashDrawerManagerIsNull_ThrowsException() {
        try {
            new CashDrawerController(null);
        } catch (IllegalArgumentException iae) {
            assertEquals("cashDrawerManager cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenCashDrawerManagerIsNew_DoesNotThrowException() {
        try {
            new CashDrawerController(mockCashDrawerManager);
        } catch (Exception exception) {
            fail("Existing Manager Argument should not result in an Exception");
        }
    }

    @Test
    public void openCashDrawer_CallsThroughToCashDrawerManager() throws DeviceException {
        cashDrawerController.openCashDrawer(DRAWER_ID);

        verify(mockCashDrawerManager).openCashDrawer(DRAWER_ID);
    }

    @Test
    public void openCashDrawer_WhenThrowsError() throws DeviceException {
        doThrow(new DeviceException(DeviceError.DEVICE_BUSY)).when(mockCashDrawerManager).openCashDrawer(DRAWER_ID);

        try {
            cashDrawerController.openCashDrawer(DRAWER_ID);
        } catch (DeviceException deviceException) {
            verify(mockCashDrawerManager).openCashDrawer(DRAWER_ID);
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void reconnect_CallsThroughToCashDrawerManager() throws DeviceException {
        cashDrawerController.reconnect(DRAWER_ID);

        verify(mockCashDrawerManager).reconnectDevice(DRAWER_ID);
    }

    @Test
    public void reconnect_WhenThrowsError() throws DeviceException {
        doThrow(new DeviceException(DeviceError.DEVICE_BUSY)).when(mockCashDrawerManager).reconnectDevice(DRAWER_ID);

        try {
            cashDrawerController.reconnect(DRAWER_ID);
        } catch (DeviceException deviceException) {
            verify(mockCashDrawerManager).reconnectDevice(DRAWER_ID);
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void getAllHealth_ReturnsHealthListFromManager() {
        List<DeviceHealthResponse> expected = List.of(new DeviceHealthResponse("cashDrawer", DeviceHealth.READY));
        when(mockCashDrawerManager.getAllHealth()).thenReturn(expected);

        List<DeviceHealthResponse> actual = cashDrawerController.getAllHealth();

        assertEquals(expected, actual);
        verify(mockCashDrawerManager).getAllHealth();
    }

    @Test
    public void getHealth_ReturnsHealthFromManager() throws DeviceException {
        DeviceHealthResponse expected = new DeviceHealthResponse("cashDrawer", DeviceHealth.READY);
        when(mockCashDrawerManager.getHealth(DRAWER_ID)).thenReturn(expected);

        DeviceHealthResponse actual = cashDrawerController.getHealth(DRAWER_ID);

        assertEquals(expected, actual);
        verify(mockCashDrawerManager).getHealth(DRAWER_ID);
    }

    @Test
    public void getStatus_ReturnsStatusFromManager() throws DeviceException {
        DeviceHealthResponse expected = new DeviceHealthResponse("cashDrawer", DeviceHealth.READY);
        when(mockCashDrawerManager.getStatus(DRAWER_ID)).thenReturn(expected);

        DeviceHealthResponse actual = cashDrawerController.getStatus(DRAWER_ID);

        assertEquals(expected, actual);
        verify(mockCashDrawerManager).getStatus(DRAWER_ID);
    }
}
