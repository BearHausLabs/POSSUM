package com.target.devicemanager.components.toneindicator;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.toneindicator.entities.ToneRequest;
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
public class ToneIndicatorManagerTest {

    private ToneIndicatorManager toneIndicatorManager;

    private ToneIndicatorManager toneIndicatorManagerCache;

    @Mock
    private ToneIndicatorDevice mockToneIndicatorDevice;
    @Mock
    private Lock mockToneIndicatorLock;
    @Mock
    private CacheManager mockCacheManager;

    private final Cache testCache = new Cache() {
        final Map<Object, Object> cacheMap = new HashMap<>();

        @Override
        public String getName() {
            return "toneIndicatorHealth";
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
        toneIndicatorManager = new ToneIndicatorManager(mockToneIndicatorDevice, mockToneIndicatorLock);
        toneIndicatorManagerCache = new ToneIndicatorManager(mockToneIndicatorDevice, mockToneIndicatorLock, mockCacheManager);
    }

    @Test
    public void ctor_WhenToneIndicatorDeviceAndLockAreNull_ThrowsException() {
        try {
            new ToneIndicatorManager(null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("toneIndicatorDevice cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenToneIndicatorDeviceIsNull_ThrowsException() {
        try {
            new ToneIndicatorManager(null, mockToneIndicatorLock);
        } catch (IllegalArgumentException iae) {
            assertEquals("toneIndicatorDevice cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenToneIndicatorLockIsNull_ThrowsException() {
        try {
            new ToneIndicatorManager(mockToneIndicatorDevice, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("toneIndicatorLock cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenToneIndicatorDeviceAndLockAreNotNull_DoesNotThrowException() {
        try {
            new ToneIndicatorManager(mockToneIndicatorDevice, mockToneIndicatorLock);
        } catch (Exception exception) {
            fail("Existing Device Argument should not result in an Exception");
        }
    }

    @Test
    public void connect_WhenLockSucceeds_Connects() {
        //arrange
        when(mockToneIndicatorDevice.tryLock()).thenReturn(true);

        //act
        toneIndicatorManager.connect();

        //assert
        verify(mockToneIndicatorDevice).connect();
        verify(mockToneIndicatorDevice).unlock();
    }

    @Test
    public void connect_WhenLockFails_DoesNotConnect() {
        //arrange
        when(mockToneIndicatorDevice.tryLock()).thenReturn(false);

        //act
        toneIndicatorManager.connect();

        //assert
        verify(mockToneIndicatorDevice, never()).connect();
        verify(mockToneIndicatorDevice, never()).unlock();
    }

    @Test
    public void reconnect_WhenLockSucceeds_Reconnects() {
        //arrange
        when(mockToneIndicatorDevice.tryLock()).thenReturn(true);
        when(mockToneIndicatorDevice.connect()).thenReturn(true);

        //act
        try {
            toneIndicatorManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            fail("reconnectDevice should not result in an Exception");
        }

        //assert
        verify(mockToneIndicatorDevice).disconnect();
        verify(mockToneIndicatorDevice).connect();
        verify(mockToneIndicatorDevice).unlock();
    }

    @Test
    public void reconnect_WhenLockFails_DoesNotReconnect() {
        //arrange
        when(mockToneIndicatorDevice.tryLock()).thenReturn(false);

        //act
        try {
            toneIndicatorManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            verify(mockToneIndicatorDevice, never()).disconnect();
            verify(mockToneIndicatorDevice, never()).connect();
            verify(mockToneIndicatorDevice, never()).unlock();
            return;
        }

        //assert
        fail("Expected DEVICE_BUSY, but got none");
    }

    @Test
    public void reconnect_WhenDeviceConnectFails_DoesNotReconnect() {
        //arrange
        when(mockToneIndicatorDevice.tryLock()).thenReturn(true);
        when(mockToneIndicatorDevice.connect()).thenReturn(false);

        //act
        try {
            toneIndicatorManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            verify(mockToneIndicatorDevice).disconnect();
            verify(mockToneIndicatorDevice).connect();
            verify(mockToneIndicatorDevice).unlock();
            return;
        }

        //assert
        fail("Expected DEVICE_OFFLINE, but got none");
    }

    @Test
    public void playSound_WhenLockFails_ThrowsException() {
        //arrange
        when(mockToneIndicatorLock.tryLock()).thenReturn(false);
        ToneRequest toneRequest = new ToneRequest(1500, 100, 50);

        //act
        try {
            toneIndicatorManager.playSound(toneRequest);
        }
        //assert
        catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected BUSY, but got none.");
    }

    @Test
    public void playSound_WhenLockSucceeds_DoesNotThrowException() throws JposException, DeviceException {
        //arrange
        when(mockToneIndicatorLock.tryLock()).thenReturn(true);
        ToneRequest toneRequest = new ToneRequest(1500, 100, 50);

        //act
        try {
            toneIndicatorManager.playSound(toneRequest);
        }
        //assert
        catch (DeviceException deviceException) {
            fail("Lock Success should not result in Exception");
        }
        verify(mockToneIndicatorDevice).playSound(toneRequest);
        verify(mockToneIndicatorLock).unlock();
    }

    @Test
    public void playSound_WhenDeviceIsOffline_ThrowsJposOfflineException() throws JposException, DeviceException {
        //arrange
        when(mockToneIndicatorLock.tryLock()).thenReturn(true);
        ToneRequest toneRequest = new ToneRequest(1500, 100, 50);
        doThrow(new JposException(JposConst.JPOS_E_OFFLINE)).when(mockToneIndicatorDevice).playSound(toneRequest);

        //act
        try {
            toneIndicatorManager.playSound(toneRequest);
        }
        //assert
        catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            return;
        }
        fail("Expected OFFLINE Exception, but got none.");
    }

    @Test
    public void playSound_WhenDeviceThrowsDeviceException_ThrowsDeviceException() throws JposException, DeviceException {
        //arrange
        when(mockToneIndicatorLock.tryLock()).thenReturn(true);
        ToneRequest toneRequest = new ToneRequest(1500, 100, 50);
        doThrow(new DeviceException(DeviceError.DEVICE_OFFLINE)).when(mockToneIndicatorDevice).playSound(toneRequest);

        //act
        try {
            toneIndicatorManager.playSound(toneRequest);
        }
        //assert
        catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            return;
        }
        fail("Expected OFFLINE Exception, but got none.");
    }

    @Test
    public void getHealth_WhenDeviceOffline_ShouldReturnNotReadyHealthResponse() {
        //arrange
        when(mockToneIndicatorDevice.isConnected()).thenReturn(false);
        when(mockToneIndicatorDevice.getDeviceName()).thenReturn("toneIndicator");
        when(mockCacheManager.getCache("toneIndicatorHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("toneIndicator", DeviceHealth.NOTREADY);

        //act
        DeviceHealthResponse deviceHealthResponse = toneIndicatorManagerCache.getHealth();

        //assert
        assertEquals("toneIndicator", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.NOTREADY, deviceHealthResponse.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_WhenDeviceOnline_ShouldReturnReadyHealthResponse() {
        //arrange
        when(mockToneIndicatorDevice.isConnected()).thenReturn(true);
        when(mockToneIndicatorDevice.getDeviceName()).thenReturn("toneIndicator");
        when(mockCacheManager.getCache("toneIndicatorHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("toneIndicator", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = toneIndicatorManagerCache.getHealth();

        //assert
        assertEquals("toneIndicator", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponse.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_WhenCacheFails_ShouldReturnReadyHealthResponse() {
        //arrange
        when(mockToneIndicatorDevice.isConnected()).thenReturn(true);
        when(mockToneIndicatorDevice.getDeviceName()).thenReturn("toneIndicator");
        when(mockCacheManager.getCache("toneIndicatorHealth")).thenReturn(null);

        //act
        DeviceHealthResponse deviceHealthResponse = toneIndicatorManagerCache.getHealth();

        //assert
        assertEquals("toneIndicator", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponse.getHealthStatus());
    }

    @Test
    public void getStatus_WhenCacheExists() {
        //arrange
        when(mockCacheManager.getCache("toneIndicatorHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("toneIndicator", DeviceHealth.READY);
        testCache.put("health", expected);

        //act
        DeviceHealthResponse deviceHealthResponse = toneIndicatorManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheExists_WhenCheckHealthFlag() {
        //arrange
        when(mockCacheManager.getCache("toneIndicatorHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("toneIndicator", DeviceHealth.READY);
        testCache.put("health", expected);

        toneIndicatorManagerCache.connect(); //set check health flag to CHECK_HEALTH
        when(mockToneIndicatorDevice.isConnected()).thenReturn(true);
        when(mockToneIndicatorDevice.getDeviceName()).thenReturn("toneIndicator");

        //act
        DeviceHealthResponse deviceHealthResponse = toneIndicatorManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOffline() {
        //arrange
        when(mockToneIndicatorDevice.isConnected()).thenReturn(false);
        when(mockToneIndicatorDevice.getDeviceName()).thenReturn("toneIndicator");
        when(mockCacheManager.getCache("toneIndicatorHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("toneIndicator", DeviceHealth.NOTREADY);

        //act
        DeviceHealthResponse deviceHealthResponse = toneIndicatorManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOnline() {
        //arrange
        when(mockToneIndicatorDevice.isConnected()).thenReturn(true);
        when(mockToneIndicatorDevice.getDeviceName()).thenReturn("toneIndicator");
        when(mockCacheManager.getCache("toneIndicatorHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("toneIndicator", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = toneIndicatorManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_CallHealth() {
        //arrange
        when(mockToneIndicatorDevice.isConnected()).thenReturn(true);
        when(mockToneIndicatorDevice.getDeviceName()).thenReturn("toneIndicator");
        when(mockCacheManager.getCache("toneIndicatorHealth")).thenReturn(null);
        DeviceHealthResponse expected = new DeviceHealthResponse("toneIndicator", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = toneIndicatorManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }
}
