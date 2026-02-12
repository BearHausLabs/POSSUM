package com.target.devicemanager.components.msr;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.msr.entities.CardData;
import com.target.devicemanager.components.msr.entities.MSRError;
import com.target.devicemanager.components.msr.entities.MSRException;
import jpos.JposConst;
import jpos.JposException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MSRManagerTest {

    private MSRManager msrManager;
    private MSRManager msrManagerCache;

    @Mock
    private MSRDevice mockMSRDevice;
    @Mock
    private Lock mockMSRLock;
    @Mock
    private CacheManager mockCacheManager;

    private final Cache testCache = new Cache() {
        final Map<Object, Object> cacheMap = new HashMap<>();

        @Override
        public String getName() {
            return "msrHealth";
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
        msrManager = new MSRManager(mockMSRDevice, mockMSRLock);
        msrManagerCache = Mockito.spy(new MSRManager(mockMSRDevice, mockMSRLock, mockCacheManager));
    }

    @Test
    public void ctor_WhenMSRDeviceAndLockAreNull_ThrowsException() {
        try {
            new MSRManager(null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("msrDevice cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenMSRDeviceIsNull_ThrowsException() {
        try {
            new MSRManager(null, mockMSRLock);
        } catch (IllegalArgumentException iae) {
            assertEquals("msrDevice cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenMSRLockIsNull_ThrowsException() {
        try {
            new MSRManager(mockMSRDevice, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("msrLock cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenMSRDeviceAndLockAreNotNull_DoesNotThrowException() {
        try {
            new MSRManager(mockMSRDevice, mockMSRLock);
        } catch (Exception exception) {
            fail("Existing Device Argument should not result in an Exception");
        }
    }

    @Test
    public void connect_CallsDeviceConnect() {
        //arrange

        //act
        msrManager.connect();

        //assert
        verify(mockMSRDevice).connect();
    }

    @Test
    public void reconnectDevice_WhenTryLockSucceeds_Reconnects() throws DeviceException {
        //arrange
        when(mockMSRDevice.tryLock()).thenReturn(true);
        when(mockMSRDevice.reconnect()).thenReturn(true);

        //act
        try {
            msrManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            fail("msrManager.reconnectDevice() should not result in an Exception");
        }

        //assert
        verify(mockMSRDevice).reconnect();
        verify(mockMSRDevice).unlock();
    }

    @Test
    public void reconnectDevice_WhenReconnectFails_ThrowsError() throws DeviceException {
        //arrange
        when(mockMSRDevice.tryLock()).thenReturn(true);
        when(mockMSRDevice.reconnect()).thenReturn(false);

        //act
        try {
            msrManager.reconnectDevice();
        }

        //assert
        catch (DeviceException deviceException) {
            verify(mockMSRDevice).reconnect();
            verify(mockMSRDevice).unlock();
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void reconnectDevice_WhenTryLockFails_ThrowsError() throws DeviceException {
        //arrange
        when(mockMSRDevice.tryLock()).thenReturn(false);

        //act
        try {
            msrManager.reconnectDevice();
        }

        //assert
        catch (DeviceException deviceException) {
            verify(mockMSRDevice, never()).reconnect();
            verify(mockMSRDevice, never()).unlock();
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void getData_WhenAlreadyLocked_ThrowsException() {
        //arrange
        when(mockMSRLock.tryLock()).thenReturn(false);

        //act
        try {
            msrManager.getData();
        }

        //assert
        catch (MSRException msrException) {
            verify(mockMSRLock, never()).unlock();
            assertEquals(DeviceError.DEVICE_BUSY, msrException.getDeviceError());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void getData_WhenSuccess_ReturnsCardData() throws MSRException, JposException {
        //arrange
        when(mockMSRLock.tryLock()).thenReturn(true);
        CardData expected = new CardData("T1", "T2", "T3", "T4");
        when(mockMSRDevice.getCardData()).thenReturn(expected);

        //act
        CardData actual = msrManager.getData();

        //assert
        assertEquals(expected, actual);
        verify(mockMSRLock).unlock();
    }

    @Test
    public void getData_WhenDeviceThrowsJposException_ThrowsMSRException() throws JposException {
        //arrange
        when(mockMSRLock.tryLock()).thenReturn(true);
        when(mockMSRDevice.getCardData()).thenThrow(new JposException(JposConst.JPOS_E_DISABLED));

        //act
        try {
            msrManager.getData();
        }

        //assert
        catch (MSRException msrException) {
            verify(mockMSRLock).unlock();
            assertEquals(MSRError.DISABLED, msrException.getDeviceError());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void cancelReadRequest_WhenAlreadyLocked_ThrowsException() {
        //arrange
        when(mockMSRLock.tryLock()).thenReturn(true);

        //act
        try {
            msrManager.cancelReadRequest();
        }

        //assert
        catch (MSRException msrException) {
            verify(mockMSRDevice, never()).cancelCardData();
            verify(mockMSRLock).unlock();
            assertEquals(MSRError.ALREADY_DISABLED, msrException.getDeviceError());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void cancelReadRequest_WhenCancelSucceeds() throws MSRException {
        //arrange
        when(mockMSRLock.tryLock()).thenReturn(false);

        //act
        msrManager.cancelReadRequest();

        //assert
        verify(mockMSRDevice).cancelCardData();
    }

    @Test
    public void getHealth_WhenDeviceOffline_ShouldReturnNotReadyHealthResponse() {
        //arrange
        when(mockMSRDevice.isConnected()).thenReturn(false);
        when(mockMSRDevice.getDeviceName()).thenReturn("MSR");
        when(mockCacheManager.getCache("msrHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("MSR", DeviceHealth.NOTREADY);

        //act
        DeviceHealthResponse actual = msrManagerCache.getHealth();

        //assert
        assertEquals("MSR", actual.getDeviceName());
        assertEquals(DeviceHealth.NOTREADY, actual.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_WhenDeviceOnline_ShouldReturnReadyHealthResponse() {
        //arrange
        when(mockMSRDevice.isConnected()).thenReturn(true);
        when(mockMSRDevice.getDeviceName()).thenReturn("MSR");
        when(mockCacheManager.getCache("msrHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("MSR", DeviceHealth.READY);

        //act
        DeviceHealthResponse actual = msrManagerCache.getHealth();

        //assert
        assertEquals("MSR", actual.getDeviceName());
        assertEquals(DeviceHealth.READY, actual.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_CacheNull_ShouldReturnHealthResponse() {
        //arrange
        when(mockMSRDevice.isConnected()).thenReturn(true);
        when(mockMSRDevice.getDeviceName()).thenReturn("MSR");
        when(mockCacheManager.getCache("msrHealth")).thenReturn(null);

        //act
        DeviceHealthResponse actual = msrManagerCache.getHealth();

        //assert
        assertEquals("MSR", actual.getDeviceName());
        assertEquals(DeviceHealth.READY, actual.getHealthStatus());
    }

    @Test
    public void getStatus_WhenCacheExists() {
        //arrange
        when(mockCacheManager.getCache("msrHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("MSR", DeviceHealth.READY);
        testCache.put("health", expected);

        //act
        DeviceHealthResponse actual = msrManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void getStatus_WhenCacheExists_WhenCheckHealthFlag() {
        //arrange
        when(mockCacheManager.getCache("msrHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("MSR", DeviceHealth.READY);
        testCache.put("health", expected);

        msrManagerCache.connect(); //set check health flag to CHECK_HEALTH
        when(mockMSRDevice.isConnected()).thenReturn(true);
        when(mockMSRDevice.getDeviceName()).thenReturn("MSR");

        //act
        DeviceHealthResponse actual = msrManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void getStatus_WhenCacheNull() {
        //arrange
        when(mockCacheManager.getCache("msrHealth")).thenReturn(null);
        when(mockMSRDevice.isConnected()).thenReturn(true);
        when(mockMSRDevice.getDeviceName()).thenReturn("MSR");

        //act
        DeviceHealthResponse actual = msrManagerCache.getStatus();

        //assert
        assertEquals("MSR", actual.getDeviceName());
        assertEquals(DeviceHealth.READY, actual.getHealthStatus());
    }
}
