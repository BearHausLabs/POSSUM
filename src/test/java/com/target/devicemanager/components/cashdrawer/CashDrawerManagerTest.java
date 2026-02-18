package com.target.devicemanager.components.cashdrawer;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.cashdrawer.entities.CashDrawerError;
import jpos.JposConst;
import jpos.JposException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CashDrawerManagerTest {

    private static final int DRAWER_ID = 1;

    private CashDrawerManager cashDrawerManager;

    private CashDrawerManager cashDrawerManagerCache;

    @Mock
    private CashDrawerDevice mockCashDrawerDevice;
    @Mock
    private Lock mockCashDrawerLock;
    @Mock
    private CacheManager mockCacheManager;

    private final Cache testCache = new Cache() {
        final Map<Object, Object> cacheMap = new HashMap<>();

        @Override
        public String getName() {
            return "cashDrawer1Health";
        }

        @Override
        public Object getNativeCache() {
            return null;
        }

        @Override
        public ValueWrapper get(Object key) {
            if (cacheMap.containsKey(key)) {
                return () -> cacheMap.get(key);
            } else {
                return null;
            }
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            return null;
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            return null;
        }

        @Override
        public void put(Object key, Object value) {
            cacheMap.put(key, value);
        }

        @Override
        public void evict(Object key) {

        }

        @Override
        public void clear() {

        }
    };

    @BeforeEach
    public void testInitialize() {
        Map<Integer, CashDrawerDevice> devices = Map.of(DRAWER_ID, mockCashDrawerDevice);
        cashDrawerManager = new CashDrawerManager(devices, mockCashDrawerLock);
        cashDrawerManagerCache = new CashDrawerManager(devices, mockCashDrawerLock, mockCacheManager);
    }

    @Test
    public void ctor_WhenCashDrawerDevicesAndLockAreNull_ThrowsException() {
        try {
            new CashDrawerManager(null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("cashDrawerDevices cannot be null or empty", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenCashDrawerDevicesIsNull_ThrowsException() {
        try {
            new CashDrawerManager(null, mockCashDrawerLock);
        } catch (IllegalArgumentException iae) {
            assertEquals("cashDrawerDevices cannot be null or empty", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenCashDrawerDevicesIsEmpty_ThrowsException() {
        try {
            new CashDrawerManager(Map.of(), mockCashDrawerLock);
        } catch (IllegalArgumentException iae) {
            assertEquals("cashDrawerDevices cannot be null or empty", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenCashDrawerLockIsNull_ThrowsException() {
        try {
            new CashDrawerManager(Map.of(DRAWER_ID, mockCashDrawerDevice), null);
        } catch (IllegalArgumentException iae) {
            assertEquals("cashDrawerLock cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenCashDrawerDevicesAndLockAreNotNull_DoesNotThrowException() {
        try {
            new CashDrawerManager(Map.of(DRAWER_ID, mockCashDrawerDevice), mockCashDrawerLock);
        } catch (Exception exception) {
            fail("Existing Device Argument should not result in an Exception");
        }
    }

    @Test
    public void connect_WhenLockSucceeds_Connects() {
        when(mockCashDrawerDevice.tryLock()).thenReturn(true);

        cashDrawerManager.connect();

        verify(mockCashDrawerDevice).connect();
        verify(mockCashDrawerDevice).unlock();
    }

    @Test
    public void connect_WhenLockFails_DoesNotConnect() {
        when(mockCashDrawerDevice.tryLock()).thenReturn(false);

        cashDrawerManager.connect();

        verify(mockCashDrawerDevice, never()).connect();
        verify(mockCashDrawerDevice, never()).unlock();
    }

    @Test
    public void reconnect_WhenLockSucceeds_Reconnects() throws DeviceException {
        when(mockCashDrawerDevice.tryLock()).thenReturn(true);
        when(mockCashDrawerDevice.connect()).thenReturn(true);

        cashDrawerManager.reconnectDevice(DRAWER_ID);

        verify(mockCashDrawerDevice).disconnect();
        verify(mockCashDrawerDevice).connect();
        verify(mockCashDrawerDevice).unlock();
    }

    @Test
    public void reconnect_WhenLockFails_DoesNotReconnect() {
        when(mockCashDrawerDevice.tryLock()).thenReturn(false);

        try {
            cashDrawerManager.reconnectDevice(DRAWER_ID);
        } catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            verify(mockCashDrawerDevice, never()).disconnect();
            verify(mockCashDrawerDevice, never()).connect();
            verify(mockCashDrawerDevice, never()).unlock();
            return;
        }

        fail("Expected DEVICE_BUSY, but got none");
    }

    @Test
    public void reconnect_WhenDeviceConnectFails_DoesNotReconnect() {
        when(mockCashDrawerDevice.tryLock()).thenReturn(true);
        when(mockCashDrawerDevice.connect()).thenReturn(false);

        try {
            cashDrawerManager.reconnectDevice(DRAWER_ID);
        } catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            verify(mockCashDrawerDevice).disconnect();
            verify(mockCashDrawerDevice).connect();
            verify(mockCashDrawerDevice).unlock();
            return;
        }

        fail("Expected DEVICE_OFFLINE, but got none");
    }

    @Test
    public void openCashDrawer_WhenLockFails_ThrowsException() {
        when(mockCashDrawerLock.tryLock()).thenReturn(false);

        try {
            cashDrawerManager.openCashDrawer(DRAWER_ID);
        } catch (DeviceException deviceException) {
            assertEquals(CashDrawerError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected BUSY, but got none.");
    }

    @Test
    public void openCashDrawer_WhenLockSucceeds_DoesNotThrowException() throws JposException, DeviceException {
        when(mockCashDrawerLock.tryLock()).thenReturn(true);

        cashDrawerManager.openCashDrawer(DRAWER_ID);

        verify(mockCashDrawerDevice).openCashDrawer();
        verify(mockCashDrawerLock).unlock();
    }

    @Test
    public void openCashDrawer_WhenCashDrawerIsOffline_ThrowsJposOfflineException() throws JposException, DeviceException {
        when(mockCashDrawerLock.tryLock()).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_OFFLINE)).when(mockCashDrawerDevice).openCashDrawer();

        try {
            cashDrawerManager.openCashDrawer(DRAWER_ID);
        } catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            return;
        }
        fail("Expected OFFLINE Exception, but got none.");
    }

    @Test
    public void openCashDrawer_WhenCashDrawerIsOffline_ThrowsDeviceOfflineException() throws JposException, DeviceException {
        when(mockCashDrawerLock.tryLock()).thenReturn(true);
        doThrow(new DeviceException(DeviceError.DEVICE_OFFLINE)).when(mockCashDrawerDevice).openCashDrawer();

        try {
            cashDrawerManager.openCashDrawer(DRAWER_ID);
        } catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            return;
        }
        fail("Expected OFFLINE Exception, but got none.");
    }

    @Test
    public void getHealth_WhenDeviceOffline_ShouldReturnNotReadyHealthResponse() throws DeviceException {
        when(mockCashDrawerDevice.isConnected()).thenReturn(false);
        when(mockCashDrawerDevice.getDeviceName()).thenReturn("cashDrawer");
        when(mockCacheManager.getCache("cashDrawer1Health")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("cashDrawer", DeviceHealth.NOTREADY);

        DeviceHealthResponse deviceHealthResponse = cashDrawerManagerCache.getHealth(DRAWER_ID);

        assertEquals("cashDrawer", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.NOTREADY, deviceHealthResponse.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_WhenDeviceOnline_ShouldReturnReadyHealthResponse() throws DeviceException {
        when(mockCashDrawerDevice.isConnected()).thenReturn(true);
        when(mockCashDrawerDevice.getDeviceName()).thenReturn("cashDrawer");
        when(mockCacheManager.getCache("cashDrawer1Health")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("cashDrawer", DeviceHealth.READY);

        DeviceHealthResponse deviceHealthResponse = cashDrawerManagerCache.getHealth(DRAWER_ID);

        assertEquals("cashDrawer", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponse.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_WhenCacheFails_ShouldReturnReadyHealthResponse() throws DeviceException {
        when(mockCashDrawerDevice.isConnected()).thenReturn(true);
        when(mockCashDrawerDevice.getDeviceName()).thenReturn("cashDrawer");
        when(mockCacheManager.getCache("cashDrawer1Health")).thenReturn(null);

        DeviceHealthResponse deviceHealthResponse = cashDrawerManagerCache.getHealth(DRAWER_ID);

        assertEquals("cashDrawer", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponse.getHealthStatus());
    }

    @Test
    public void getStatus_WhenCacheExists() throws DeviceException {
        when(mockCacheManager.getCache("cashDrawer1Health")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("cashDrawer", DeviceHealth.READY);
        testCache.put("health", expected);

        DeviceHealthResponse deviceHealthResponse = cashDrawerManagerCache.getStatus(DRAWER_ID);

        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheExists_WhenCheckHealthFlag() throws DeviceException {
        when(mockCacheManager.getCache("cashDrawer1Health")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("cashDrawer", DeviceHealth.READY);
        testCache.put("health", expected);

        cashDrawerManagerCache.connect();
        when(mockCashDrawerDevice.isConnected()).thenReturn(true);
        when(mockCashDrawerDevice.getDeviceName()).thenReturn("cashDrawer");

        DeviceHealthResponse deviceHealthResponse = cashDrawerManagerCache.getStatus(DRAWER_ID);

        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOffline() throws DeviceException {
        when(mockCashDrawerDevice.isConnected()).thenReturn(false);
        when(mockCashDrawerDevice.getDeviceName()).thenReturn("cashDrawer");
        when(mockCacheManager.getCache("cashDrawer1Health")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("cashDrawer", DeviceHealth.NOTREADY);

        DeviceHealthResponse deviceHealthResponse = cashDrawerManagerCache.getStatus(DRAWER_ID);

        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOnline() throws DeviceException {
        when(mockCashDrawerDevice.isConnected()).thenReturn(true);
        when(mockCashDrawerDevice.getDeviceName()).thenReturn("cashDrawer");
        when(mockCacheManager.getCache("cashDrawer1Health")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("cashDrawer", DeviceHealth.READY);

        DeviceHealthResponse deviceHealthResponse = cashDrawerManagerCache.getStatus(DRAWER_ID);

        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_CallHealth() throws DeviceException {
        when(mockCashDrawerDevice.isConnected()).thenReturn(true);
        when(mockCashDrawerDevice.getDeviceName()).thenReturn("cashDrawer");
        when(mockCacheManager.getCache("cashDrawer1Health")).thenReturn(null);

        DeviceHealthResponse deviceHealthResponse = cashDrawerManagerCache.getStatus(DRAWER_ID);

        assertEquals("cashDrawer", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponse.getHealthStatus());
    }

    @Test
    public void getAllHealth_ReturnsListWithOneDrawer() {
        when(mockCashDrawerDevice.isConnected()).thenReturn(true);
        when(mockCashDrawerDevice.getDeviceName()).thenReturn("cashDrawer");
        when(mockCacheManager.getCache("cashDrawer1Health")).thenReturn(testCache);

        var responses = cashDrawerManagerCache.getAllHealth();

        assertEquals(1, responses.size());
        assertEquals("cashDrawer", responses.get(0).getDeviceName());
        assertEquals(DeviceHealth.READY, responses.get(0).getHealthStatus());
    }
}
