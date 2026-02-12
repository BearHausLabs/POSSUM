package com.target.devicemanager.components.poskeyboard;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.poskeyboard.entities.KeyboardEventData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class POSKeyboardManagerTest {

    private POSKeyboardManager posKeyboardManager;
    private POSKeyboardManager posKeyboardManagerCache;

    @Mock
    private POSKeyboardDevice mockPosKeyboardDevice;
    @Mock
    private Lock mockPosKeyboardLock;
    @Mock
    private CacheManager mockCacheManager;

    private final Cache testCache = new Cache() {
        final Map<Object, Object> cacheMap = new HashMap<>();

        @Override
        public String getName() {
            return "posKeyboardHealth";
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
        posKeyboardManager = new POSKeyboardManager(mockPosKeyboardDevice, mockPosKeyboardLock);
        posKeyboardManagerCache = new POSKeyboardManager(mockPosKeyboardDevice, mockPosKeyboardLock, mockCacheManager);
    }

    @Test
    public void ctor_WhenDeviceAndLockAreNull_ThrowsException() {
        try {
            new POSKeyboardManager(null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("posKeyboardDevice cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDeviceIsNull_ThrowsException() {
        try {
            new POSKeyboardManager(null, mockPosKeyboardLock);
        } catch (IllegalArgumentException iae) {
            assertEquals("posKeyboardDevice cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenLockIsNull_ThrowsException() {
        try {
            new POSKeyboardManager(mockPosKeyboardDevice, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("posKeyboardLock cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDeviceAndLockAreNotNull_DoesNotThrowException() {
        try {
            new POSKeyboardManager(mockPosKeyboardDevice, mockPosKeyboardLock);
        } catch (Exception exception) {
            fail("Existing Device Argument should not result in an Exception");
        }
    }

    @Test
    public void connect_WhenLockSucceeds_Connects() {
        //arrange
        when(mockPosKeyboardDevice.tryLock()).thenReturn(true);

        //act
        posKeyboardManager.connect();

        //assert
        verify(mockPosKeyboardDevice).connect();
        verify(mockPosKeyboardDevice).unlock();
    }

    @Test
    public void connect_WhenLockFails_DoesNotConnect() {
        //arrange
        when(mockPosKeyboardDevice.tryLock()).thenReturn(false);

        //act
        posKeyboardManager.connect();

        //assert
        verify(mockPosKeyboardDevice, never()).connect();
        verify(mockPosKeyboardDevice, never()).unlock();
    }

    @Test
    public void reconnect_WhenLockSucceeds_Reconnects() {
        //arrange
        when(mockPosKeyboardDevice.tryLock()).thenReturn(true);
        when(mockPosKeyboardDevice.connect()).thenReturn(true);

        //act
        try {
            posKeyboardManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            fail("reconnectDevice should not result in an Exception");
        }

        //assert
        verify(mockPosKeyboardDevice).disconnect();
        verify(mockPosKeyboardDevice).connect();
        verify(mockPosKeyboardDevice).unlock();
    }

    @Test
    public void reconnect_WhenLockFails_DoesNotReconnect() {
        //arrange
        when(mockPosKeyboardDevice.tryLock()).thenReturn(false);

        //act
        try {
            posKeyboardManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            verify(mockPosKeyboardDevice, never()).disconnect();
            verify(mockPosKeyboardDevice, never()).connect();
            verify(mockPosKeyboardDevice, never()).unlock();
            return;
        }

        fail("Expected DEVICE_BUSY, but got none");
    }

    @Test
    public void reconnect_WhenDeviceConnectFails_DoesNotReconnect() {
        //arrange
        when(mockPosKeyboardDevice.tryLock()).thenReturn(true);
        when(mockPosKeyboardDevice.connect()).thenReturn(false);

        //act
        try {
            posKeyboardManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            verify(mockPosKeyboardDevice).disconnect();
            verify(mockPosKeyboardDevice).connect();
            verify(mockPosKeyboardDevice).unlock();
            return;
        }

        fail("Expected DEVICE_OFFLINE, but got none");
    }

    @Test
    public void addEventSubscriber_AddsEmitter() {
        //arrange
        SseEmitter emitter = new SseEmitter();

        //act
        posKeyboardManager.addEventSubscriber(emitter);

        //assert - no exception thrown
    }

    @Test
    public void removeEventSubscriber_RemovesEmitter() {
        //arrange
        SseEmitter emitter = new SseEmitter();
        posKeyboardManager.addEventSubscriber(emitter);

        //act
        posKeyboardManager.removeEventSubscriber(emitter);

        //assert - no exception thrown
    }

    @Test
    public void getHealth_WhenDeviceOffline_ShouldReturnNotReadyHealthResponse() {
        //arrange
        when(mockPosKeyboardDevice.isConnected()).thenReturn(false);
        when(mockPosKeyboardDevice.getDeviceName()).thenReturn("POSKeyboard");
        when(mockCacheManager.getCache("posKeyboardHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("POSKeyboard", DeviceHealth.NOTREADY);

        //act
        DeviceHealthResponse deviceHealthResponse = posKeyboardManagerCache.getHealth();

        //assert
        assertEquals("POSKeyboard", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.NOTREADY, deviceHealthResponse.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_WhenDeviceOnline_ShouldReturnReadyHealthResponse() {
        //arrange
        when(mockPosKeyboardDevice.isConnected()).thenReturn(true);
        when(mockPosKeyboardDevice.getDeviceName()).thenReturn("POSKeyboard");
        when(mockCacheManager.getCache("posKeyboardHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("POSKeyboard", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = posKeyboardManagerCache.getHealth();

        //assert
        assertEquals("POSKeyboard", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponse.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_WhenCacheFails_ShouldReturnReadyHealthResponse() {
        //arrange
        when(mockPosKeyboardDevice.isConnected()).thenReturn(true);
        when(mockPosKeyboardDevice.getDeviceName()).thenReturn("POSKeyboard");
        when(mockCacheManager.getCache("posKeyboardHealth")).thenReturn(null);

        //act
        DeviceHealthResponse deviceHealthResponse = posKeyboardManagerCache.getHealth();

        //assert
        assertEquals("POSKeyboard", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponse.getHealthStatus());
    }

    @Test
    public void getStatus_WhenCacheExists() {
        //arrange
        when(mockCacheManager.getCache("posKeyboardHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("POSKeyboard", DeviceHealth.READY);
        testCache.put("health", expected);

        //act
        DeviceHealthResponse deviceHealthResponse = posKeyboardManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheExists_WhenCheckHealthFlag() {
        //arrange
        when(mockCacheManager.getCache("posKeyboardHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("POSKeyboard", DeviceHealth.READY);
        testCache.put("health", expected);

        posKeyboardManagerCache.connect(); // set check health flag to CHECK_HEALTH
        when(mockPosKeyboardDevice.isConnected()).thenReturn(true);
        when(mockPosKeyboardDevice.getDeviceName()).thenReturn("POSKeyboard");

        //act
        DeviceHealthResponse deviceHealthResponse = posKeyboardManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOffline() {
        //arrange
        when(mockPosKeyboardDevice.isConnected()).thenReturn(false);
        when(mockPosKeyboardDevice.getDeviceName()).thenReturn("POSKeyboard");
        when(mockCacheManager.getCache("posKeyboardHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("POSKeyboard", DeviceHealth.NOTREADY);

        //act
        DeviceHealthResponse deviceHealthResponse = posKeyboardManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOnline() {
        //arrange
        when(mockPosKeyboardDevice.isConnected()).thenReturn(true);
        when(mockPosKeyboardDevice.getDeviceName()).thenReturn("POSKeyboard");
        when(mockCacheManager.getCache("posKeyboardHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("POSKeyboard", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = posKeyboardManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_CallHealth() {
        //arrange
        when(mockPosKeyboardDevice.isConnected()).thenReturn(true);
        when(mockPosKeyboardDevice.getDeviceName()).thenReturn("POSKeyboard");
        when(mockCacheManager.getCache("posKeyboardHealth")).thenReturn(null);
        DeviceHealthResponse expected = new DeviceHealthResponse("POSKeyboard", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = posKeyboardManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }
}
