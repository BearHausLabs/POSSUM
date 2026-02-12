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
public class KeylockManagerTest {

    private KeylockManager keylockManager;

    private KeylockManager keylockManagerCache;

    @Mock
    private KeylockDevice mockKeylockDevice;
    @Mock
    private Lock mockKeylockLock;
    @Mock
    private CacheManager mockCacheManager;

    private final Cache testCache = new Cache() {
        final Map<Object, Object> cacheMap = new HashMap<>();

        @Override
        public String getName() {
            return "keylockHealth";
        }

        @Override
        public Object getNativeCache() {
            return null;
        }

        @Override
        public ValueWrapper get(Object key) {
            if(cacheMap.containsKey(key)) {
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
        keylockManager = new KeylockManager(mockKeylockDevice, mockKeylockLock);
        keylockManagerCache = new KeylockManager(mockKeylockDevice, mockKeylockLock, mockCacheManager);
    }

    @Test
    public void ctor_WhenKeylockDeviceAndLockAreNull_ThrowsException() {
        try {
            new KeylockManager(null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("keylockDevice cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenKeylockDeviceIsNull_ThrowsException() {
        try {
            new KeylockManager(null, mockKeylockLock);
        } catch (IllegalArgumentException iae) {
            assertEquals("keylockDevice cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenKeylockLockIsNull_ThrowsException() {
        try {
            new KeylockManager(mockKeylockDevice, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("keylockLock cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenKeylockDeviceAndLockAreNotNull_DoesNotThrowException() {
        try {
            new KeylockManager(mockKeylockDevice, mockKeylockLock);
        } catch(Exception exception) {
            fail("Existing Device Argument should not result in an Exception");
        }
    }

    @Test
    public void connect_WhenLockSucceeds_Connects() {
        //arrange
        when(mockKeylockDevice.tryLock()).thenReturn(true);

        //act
        keylockManager.connect();

        //assert
        verify(mockKeylockDevice).connect();
        verify(mockKeylockDevice).unlock();
    }

    @Test
    public void connect_WhenLockFails_DoesNotConnect() {
        //arrange
        when(mockKeylockDevice.tryLock()).thenReturn(false);

        //act
        keylockManager.connect();

        //assert
        verify(mockKeylockDevice, never()).connect();
        verify(mockKeylockDevice, never()).unlock();
    }

    @Test
    public void reconnect_WhenLockSucceeds_Reconnects() {
        //arrange
        when(mockKeylockDevice.tryLock()).thenReturn(true);
        when(mockKeylockDevice.connect()).thenReturn(true);

        //act
        try {
            keylockManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            fail("reconnectDevice should not result in an Exception");
        }

        //assert
        verify(mockKeylockDevice).disconnect();
        verify(mockKeylockDevice).connect();
        verify(mockKeylockDevice).unlock();
    }

    @Test
    public void reconnect_WhenLockFails_DoesNotReconnect() {
        //arrange
        when(mockKeylockDevice.tryLock()).thenReturn(false);

        //act
        try {
            keylockManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            verify(mockKeylockDevice, never()).disconnect();
            verify(mockKeylockDevice, never()).connect();
            verify(mockKeylockDevice, never()).unlock();
            return;
        }

        //assert
        fail("Expected DEVICE_BUSY, but got none");
    }

    @Test
    public void reconnect_WhenDeviceConnectFails_DoesNotReconnect() {
        //arrange
        when(mockKeylockDevice.tryLock()).thenReturn(true);
        when(mockKeylockDevice.connect()).thenReturn(false);

        //act
        try {
            keylockManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            verify(mockKeylockDevice).disconnect();
            verify(mockKeylockDevice).connect();
            verify(mockKeylockDevice).unlock();
            return;
        }

        //assert
        fail("Expected DEVICE_OFFLINE, but got none");
    }

    @Test
    public void getKeyPosition_WhenLockFails_ThrowsException() {
        //arrange
        when(mockKeylockLock.tryLock()).thenReturn(false);

        //act
        try {
            keylockManager.getKeyPosition();
        }
        //assert
        catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected BUSY, but got none.");
    }

    @Test
    public void getKeyPosition_WhenDeviceOffline_ThrowsException() {
        //arrange
        when(mockKeylockLock.tryLock()).thenReturn(true);
        when(mockKeylockDevice.isConnected()).thenReturn(false);

        //act
        try {
            keylockManager.getKeyPosition();
        }
        //assert
        catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            return;
        }
        fail("Expected OFFLINE, but got none.");
    }

    @Test
    public void getKeyPosition_WhenLockSucceeds_ReturnsPosition() throws DeviceException {
        //arrange
        when(mockKeylockLock.tryLock()).thenReturn(true);
        when(mockKeylockDevice.isConnected()).thenReturn(true);
        when(mockKeylockDevice.getKeyPosition()).thenReturn(KeylockPosition.SUPERVISOR);

        //act
        KeylockPosition position = keylockManager.getKeyPosition();

        //assert
        assertEquals(KeylockPosition.SUPERVISOR, position);
        verify(mockKeylockDevice).getKeyPosition();
        verify(mockKeylockLock).unlock();
    }

    @Test
    public void getHealth_WhenDeviceOffline_ShouldReturnNotReadyHealthResponse() {
        //arrange
        when(mockKeylockDevice.isConnected()).thenReturn(false);
        when(mockKeylockDevice.getDeviceName()).thenReturn("keylock");
        when(mockCacheManager.getCache("keylockHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("keylock", DeviceHealth.NOTREADY);

        //act
        DeviceHealthResponse deviceHealthResponse = keylockManagerCache.getHealth();

        //assert
        assertEquals("keylock", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.NOTREADY, deviceHealthResponse.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_WhenDeviceOnline_ShouldReturnReadyHealthResponse() {
        //arrange
        when(mockKeylockDevice.isConnected()).thenReturn(true);
        when(mockKeylockDevice.getDeviceName()).thenReturn("keylock");
        when(mockCacheManager.getCache("keylockHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("keylock", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = keylockManagerCache.getHealth();

        //assert
        assertEquals("keylock", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponse.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_WhenCacheFails_ShouldReturnReadyHealthResponse() {
        //arrange
        when(mockKeylockDevice.isConnected()).thenReturn(true);
        when(mockKeylockDevice.getDeviceName()).thenReturn("keylock");
        when(mockCacheManager.getCache("keylockHealth")).thenReturn(null);
        DeviceHealthResponse expected = new DeviceHealthResponse("keylock", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = keylockManagerCache.getHealth();

        //assert
        assertEquals("keylock", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponse.getHealthStatus());
    }

    @Test
    public void getStatus_WhenCacheExists() {
        //arrange
        when(mockCacheManager.getCache("keylockHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("keylock", DeviceHealth.READY);
        testCache.put("health", expected);

        //act
        DeviceHealthResponse deviceHealthResponse = keylockManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheExists_WhenCheckHealthFlag() {
        //arrange
        when(mockCacheManager.getCache("keylockHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("keylock", DeviceHealth.READY);
        testCache.put("health", expected);

        keylockManagerCache.connect(); //set check health flag to CHECK_HEALTH
        when(mockKeylockDevice.isConnected()).thenReturn(true); //make sure health returns READY
        when(mockKeylockDevice.getDeviceName()).thenReturn("keylock");

        //act
        DeviceHealthResponse deviceHealthResponse = keylockManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOffline() {
        //arrange
        when(mockKeylockDevice.isConnected()).thenReturn(false);
        when(mockKeylockDevice.getDeviceName()).thenReturn("keylock");
        when(mockCacheManager.getCache("keylockHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("keylock", DeviceHealth.NOTREADY);

        //act
        DeviceHealthResponse deviceHealthResponse = keylockManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOnline() {
        //arrange
        when(mockKeylockDevice.isConnected()).thenReturn(true);
        when(mockKeylockDevice.getDeviceName()).thenReturn("keylock");
        when(mockCacheManager.getCache("keylockHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("keylock", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = keylockManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_CallHealth() {
        //arrange
        when(mockKeylockDevice.isConnected()).thenReturn(true);
        when(mockKeylockDevice.getDeviceName()).thenReturn("keylock");
        when(mockCacheManager.getCache("keylockHealth")).thenReturn(null);
        DeviceHealthResponse expected = new DeviceHealthResponse("keylock", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = keylockManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }
}
