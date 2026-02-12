package com.target.devicemanager.components.keylock;

import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.components.keylock.entities.KeylockPosition;
import jpos.Keylock;
import jpos.KeylockConst;
import jpos.JposConst;
import jpos.JposException;
import jpos.events.StatusUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class KeylockDeviceTest {

    private KeylockDevice keylockDevice;
    private KeylockDevice keylockDeviceLock;

    @Mock
    private DynamicDevice<Keylock> mockDynamicKeylock;
    @Mock
    private Keylock mockKeylock;
    @Mock
    private StatusUpdateEvent mockStatusUpdateEvent;
    @Mock
    private DeviceListener mockDeviceListener;
    @Mock
    private ReentrantLock mockConnectLock;

    @BeforeEach
    public void testInitialize() throws Exception {
        when(mockDynamicKeylock.getDevice()).thenReturn(mockKeylock);

        keylockDevice = new KeylockDevice(mockDynamicKeylock, mockDeviceListener);
        keylockDeviceLock = new KeylockDevice(mockDynamicKeylock, mockDeviceListener, mockConnectLock);

        //Default Mock Behavior
        when(mockKeylock.getKeyPosition()).thenReturn(KeylockConst.LOCK_KP_LOCK);
    }

    @Test
    public void ctor_WhenDynamicKeylockAndDeviceListenerAreNull_ThrowsException() {
        try {
            new KeylockDevice(null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("dynamicKeylock cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicKeylockIsNull_ThrowsException() {
        try {
            new KeylockDevice(null, mockDeviceListener);
        } catch (IllegalArgumentException iae) {
            assertEquals("dynamicKeylock cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDeviceListenerIsNull_ThrowsException() {
        try {
            new KeylockDevice(mockDynamicKeylock, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("deviceListener cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicKeylockAndDeviceListenerAreNotNull_DoesNotThrowException() {
        try {
            new KeylockDevice(mockDynamicKeylock, mockDeviceListener);
        } catch(Exception exception) {
            fail("Existing Device Argument should not result in an Exception");
        }
    }

    @Test
    public void connect_CallsDynamicConnect() {
        //arrange

        //act
        assertTrue(keylockDevice.connect());

        //assert
        verify(mockDynamicKeylock).connect();
        verify(mockDynamicKeylock, times(2)).getDevice();
        assertTrue(keylockDevice.isConnected());
    }

    @Test
    public void connect_CallsDynamicConnect_ReturnsNotConnected() {
        //arrange
        when(mockDynamicKeylock.connect()).thenReturn(DynamicDevice.ConnectionResult.NOT_CONNECTED);

        //act
        assertFalse(keylockDevice.connect());

        //assert
        verify(mockDynamicKeylock).connect();
        verify(mockDynamicKeylock, never()).getDevice();
        assertFalse(keylockDevice.isConnected());
    }

    @Test
    public void connect_WhenAreListenersAttachedFalse_AttachListeners() {
        //arrange
        keylockDevice.setAreListenersAttached(false);

        //act
        assertTrue(keylockDevice.connect());

        //assert
        verify(mockDynamicKeylock).connect();
        verify(mockKeylock).addStatusUpdateListener(any());
        assertTrue(keylockDevice.getAreListenersAttached());
        verify(mockDynamicKeylock, times(2)).getDevice();
        assertTrue(keylockDevice.isConnected());
    }

    @Test
    public void connect_WhenAreListenersAttachedTrue_DoesNotAttachListeners() {
        //arrange
        keylockDevice.setAreListenersAttached(true);

        //act
        assertTrue(keylockDevice.connect());

        //assert
        verify(mockDynamicKeylock).connect();
        verify(mockKeylock, never()).addStatusUpdateListener(any());
        assertTrue(keylockDevice.getAreListenersAttached());
        verify(mockDynamicKeylock, times(1)).getDevice();
        assertTrue(keylockDevice.isConnected());
    }

    @Test
    public void connect_WhenGetDeviceEnabledFalse_SetDeviceEnabled() throws JposException {
        //arrange
        when(mockKeylock.getDeviceEnabled()).thenReturn(false);

        //act
        assertTrue(keylockDevice.connect());

        //assert
        verify(mockDynamicKeylock).connect();
        verify(mockKeylock).setDeviceEnabled(true);
        verify(mockKeylock).getKeyPosition();
        verify(mockDynamicKeylock, times(2)).getDevice();
        assertTrue(keylockDevice.isConnected());
    }

    @Test
    public void connect_WhenGetDeviceEnabledTrue_DoesNotSetDeviceEnabled() throws JposException {
        //arrange
        when(mockKeylock.getDeviceEnabled()).thenReturn(true);

        //act
        assertTrue(keylockDevice.connect());

        //assert
        verify(mockDynamicKeylock).connect();
        verify(mockKeylock, never()).setDeviceEnabled(true);
        verify(mockKeylock, never()).getKeyPosition();
        verify(mockDynamicKeylock, times(2)).getDevice();
        assertFalse(keylockDevice.isConnected());
    }

    @Test
    public void connect_WhenGetDeviceEnabledThrowsException() throws JposException {
        //arrange
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockKeylock).getDeviceEnabled();

        //act
        assertFalse(keylockDevice.connect());

        //assert
        verify(mockDynamicKeylock).connect();
        verify(mockKeylock, never()).setDeviceEnabled(true);
        verify(mockKeylock, never()).getKeyPosition();
        verify(mockDynamicKeylock, times(2)).getDevice();
        assertFalse(keylockDevice.isConnected());
    }

    @Test
    public void connect_WhenSetDeviceEnabledThrowsException() throws JposException {
        //arrange
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockKeylock).setDeviceEnabled(true);

        //act
        assertFalse(keylockDevice.connect());

        //assert
        verify(mockDynamicKeylock).connect();
        verify(mockKeylock).setDeviceEnabled(true);
        verify(mockKeylock, never()).getKeyPosition();
        verify(mockDynamicKeylock, times(2)).getDevice();
        assertFalse(keylockDevice.isConnected());
    }

    @Test
    public void connect_WhenGetKeyPositionThrowsException_DefaultsToLock() throws JposException {
        //arrange
        when(mockKeylock.getDeviceEnabled()).thenReturn(false);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockKeylock).getKeyPosition();

        //act
        assertTrue(keylockDevice.connect());

        //assert
        verify(mockKeylock).setDeviceEnabled(true);
        verify(mockKeylock).getKeyPosition();
        assertEquals(KeylockPosition.LOCKED, keylockDevice.getKeyPosition());
        assertTrue(keylockDevice.isConnected());
    }

    @Test
    public void connect_WhenGetKeyPositionReturnsNorm_SetsNormal() throws JposException {
        //arrange
        when(mockKeylock.getDeviceEnabled()).thenReturn(false);
        when(mockKeylock.getKeyPosition()).thenReturn(KeylockConst.LOCK_KP_NORM);

        //act
        assertTrue(keylockDevice.connect());

        //assert
        assertEquals(KeylockPosition.NORMAL, keylockDevice.getKeyPosition());
        assertTrue(keylockDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedTrue() throws JposException {
        //arrange
        when(mockDynamicKeylock.isConnected()).thenReturn(true);

        //act
        keylockDevice.disconnect();

        //assert
        assertFalse(keylockDevice.getAreListenersAttached());
        verify(mockKeylock, times(2)).getDeviceEnabled();
        verify(mockDynamicKeylock, times(2)).getDevice();
        assertTrue(keylockDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedTrue_WhenAreListenersAttachedFalse_DoesNotDetachListeners() throws JposException {
        //arrange
        when(mockDynamicKeylock.isConnected()).thenReturn(true);
        keylockDevice.setAreListenersAttached(false);

        //act
        keylockDevice.disconnect();

        //assert
        assertFalse(keylockDevice.getAreListenersAttached());
        verify(mockKeylock, times(2)).getDeviceEnabled();
        verify(mockDynamicKeylock, times(2)).getDevice();
        assertTrue(keylockDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedTrue_WhenAreListenersAttachedTrue_DetachListeners() throws JposException {
        //arrange
        when(mockDynamicKeylock.isConnected()).thenReturn(true);
        keylockDevice.setAreListenersAttached(true);

        //act
        keylockDevice.disconnect();

        //assert
        assertFalse(keylockDevice.getAreListenersAttached());
        verify(mockKeylock, times(2)).getDeviceEnabled();
        verify(mockDynamicKeylock, times(3)).getDevice();
        assertTrue(keylockDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedTrue_WhenGetDeviceEnabledFalse_DoesNotSetDeviceEnabled() throws JposException {
        //arrange
        when(mockDynamicKeylock.isConnected()).thenReturn(true);
        when(mockKeylock.getDeviceEnabled()).thenReturn(false);

        //act
        keylockDevice.disconnect();

        //assert
        assertFalse(keylockDevice.getAreListenersAttached());
        verify(mockKeylock, times(2)).getDeviceEnabled();
        verify(mockDynamicKeylock, times(2)).getDevice();
        verify(mockKeylock, never()).setDeviceEnabled(false);
        verify(mockDynamicKeylock, never()).disconnect();
        assertTrue(keylockDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedTrue_WhenGetDeviceEnabledTrue_SetDeviceEnabled() throws JposException {
        //arrange
        when(mockDynamicKeylock.isConnected()).thenReturn(true);
        when(mockKeylock.getDeviceEnabled()).thenReturn(true);

        //act
        keylockDevice.disconnect();

        //assert
        assertFalse(keylockDevice.getAreListenersAttached());
        verify(mockKeylock, times(2)).getDeviceEnabled();
        verify(mockDynamicKeylock, times(2)).getDevice();
        verify(mockKeylock, times(1)).setDeviceEnabled(false);
        verify(mockDynamicKeylock, times(1)).disconnect();
        assertFalse(keylockDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedTrue_WhenGetDeviceEnabledThrowsException() throws JposException {
        //arrange
        when(mockDynamicKeylock.isConnected()).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockKeylock).getDeviceEnabled();

        //act
        keylockDevice.disconnect();

        //assert
        assertFalse(keylockDevice.getAreListenersAttached());
        verify(mockKeylock, times(2)).getDeviceEnabled();
        verify(mockDynamicKeylock, times(2)).getDevice();
        verify(mockKeylock, never()).setDeviceEnabled(false);
        verify(mockDynamicKeylock, never()).disconnect();
        assertFalse(keylockDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedFalse() throws JposException {
        //arrange
        when(mockDynamicKeylock.isConnected()).thenReturn(false);
        keylockDevice.setAreListenersAttached(true);

        //act
        keylockDevice.disconnect();

        //assert
        assertTrue(keylockDevice.getAreListenersAttached());
        verify(mockKeylock, never()).setDeviceEnabled(false);
        verify(mockDynamicKeylock, times(1)).getDevice();
        verify(mockKeylock, times(1)).getDeviceEnabled();
        verify(mockKeylock, times(1)).setDeviceEnabled(true);
        verify(mockDynamicKeylock, never()).disconnect();
        verify(mockKeylock, times(1)).getKeyPosition();
        assertTrue(keylockDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedFalse_WhenGetDeviceEnabledFalse_SetDeviceEnabled() throws JposException {
        //arrange
        when(mockDynamicKeylock.isConnected()).thenReturn(false);
        when(mockKeylock.getDeviceEnabled()).thenReturn(false);

        //act
        keylockDevice.disconnect();

        //assert
        verify(mockKeylock).setDeviceEnabled(true);
        verify(mockKeylock).getKeyPosition();
        verify(mockDynamicKeylock, times(1)).getDevice();
        assertTrue(keylockDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedFalse_WhenGetDeviceEnabledTrue_DoesNotSetDeviceEnabled() throws JposException {
        //arrange
        when(mockDynamicKeylock.isConnected()).thenReturn(false);
        when(mockKeylock.getDeviceEnabled()).thenReturn(true);

        //act
        keylockDevice.disconnect();

        //assert
        verify(mockKeylock, never()).setDeviceEnabled(true);
        verify(mockKeylock, never()).getKeyPosition();
        verify(mockDynamicKeylock, times(1)).getDevice();
        assertFalse(keylockDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedFalse_WhenGetDeviceEnabledThrowsException() throws JposException {
        //arrange
        when(mockDynamicKeylock.isConnected()).thenReturn(false);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockKeylock).getDeviceEnabled();

        //act
        keylockDevice.disconnect();

        //assert
        verify(mockKeylock, never()).setDeviceEnabled(true);
        verify(mockKeylock, never()).getKeyPosition();
        verify(mockDynamicKeylock, times(1)).getDevice();
        assertFalse(keylockDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedFalse_WhenSetDeviceEnabledThrowsException() throws JposException {
        //arrange
        when(mockDynamicKeylock.isConnected()).thenReturn(false);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockKeylock).setDeviceEnabled(true);

        //act
        keylockDevice.disconnect();

        //assert
        verify(mockKeylock).setDeviceEnabled(true);
        verify(mockKeylock, never()).getKeyPosition();
        verify(mockDynamicKeylock, times(1)).getDevice();
        assertFalse(keylockDevice.isConnected());
    }

    @Test
    public void getKeyPosition_ReturnsCurrentPosition() {
        //arrange
        keylockDevice.setCurrentPosition(KeylockConst.LOCK_KP_NORM);

        //act
        KeylockPosition actual = keylockDevice.getKeyPosition();

        //assert
        assertEquals(KeylockPosition.NORMAL, actual);
    }

    @Test
    public void getKeyPosition_WhenLocked_ReturnsLocked() {
        //arrange
        keylockDevice.setCurrentPosition(KeylockConst.LOCK_KP_LOCK);

        //act
        KeylockPosition actual = keylockDevice.getKeyPosition();

        //assert
        assertEquals(KeylockPosition.LOCKED, actual);
    }

    @Test
    public void getKeyPosition_WhenSupervisor_ReturnsSupervisor() {
        //arrange
        keylockDevice.setCurrentPosition(KeylockConst.LOCK_KP_SUPR);

        //act
        KeylockPosition actual = keylockDevice.getKeyPosition();

        //assert
        assertEquals(KeylockPosition.SUPERVISOR, actual);
    }

    @Test
    public void getKeyPosition_WhenUnknown_ReturnsUnknown() {
        //arrange
        keylockDevice.setCurrentPosition(999);

        //act
        KeylockPosition actual = keylockDevice.getKeyPosition();

        //assert
        assertEquals(KeylockPosition.UNKNOWN, actual);
    }

    @Test
    public void getDeviceName_ReturnsName() {
        // arrange
        String expectedDeviceName = "keylock";
        when(mockDynamicKeylock.getDeviceName()).thenReturn(expectedDeviceName);

        //act
        String actual = keylockDevice.getDeviceName();

        // act/assert
        assertEquals(expectedDeviceName, actual);
    }

    @Test
    public void isConnected_ReturnsTrueFromDynamicDevice() {
        //arrange
        keylockDevice.setDeviceConnected(true);

        //act
        boolean actual = keylockDevice.isConnected();

        //assert
        assertTrue(actual);
    }

    @Test
    public void isConnected_ReturnsFalseFromDynamicDevice() {
        //arrange
        keylockDevice.setDeviceConnected(false);

        //act
        boolean actual = keylockDevice.isConnected();

        //assert
        assertFalse(actual);
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOff_Disconnected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFF);
        KeylockDevice keylockSpy = spy(keylockDevice);

        //act
        keylockSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(keylockSpy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOffOffline_Disconnected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFF_OFFLINE);
        KeylockDevice keylockSpy = spy(keylockDevice);

        //act
        keylockSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(keylockSpy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOffline_Disconnected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFFLINE);
        KeylockDevice keylockSpy = spy(keylockDevice);

        //act
        keylockSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(keylockSpy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOnline_Connected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_ONLINE);
        KeylockDevice keylockSpy = spy(keylockDevice);

        //act
        keylockSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertTrue(keylockSpy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenKeyLocked() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(KeylockConst.LOCK_KP_LOCK);
        KeylockDevice keylockSpy = spy(keylockDevice);

        //act
        keylockSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertEquals(KeylockPosition.LOCKED, keylockSpy.getKeyPosition());
    }

    @Test
    public void statusUpdateOccurred_WhenKeyNormal() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(KeylockConst.LOCK_KP_NORM);
        KeylockDevice keylockSpy = spy(keylockDevice);

        //act
        keylockSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertEquals(KeylockPosition.NORMAL, keylockSpy.getKeyPosition());
    }

    @Test
    public void statusUpdateOccurred_WhenKeySupervisor() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(KeylockConst.LOCK_KP_SUPR);
        KeylockDevice keylockSpy = spy(keylockDevice);

        //act
        keylockSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertEquals(KeylockPosition.SUPERVISOR, keylockSpy.getKeyPosition());
    }

    @Test
    public void statusUpdateOccurred_WhenOtherStatus_DoNothing() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(572);
        KeylockDevice keylockSpy = spy(keylockDevice);

        //act
        keylockSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        //do nothing
    }

    @Test
    public void tryLock_WhenLock_ReturnsTrue() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);

        //act
        keylockDeviceLock.tryLock();

        //assert
        assertTrue(keylockDeviceLock.getIsLocked());
    }

    @Test
    public void tryLock_WhenNotLock_ReturnsFalse() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(false);

        //act
        keylockDeviceLock.tryLock();

        //assert
        assertFalse(keylockDeviceLock.getIsLocked());
    }

    @Test
    public void tryLock_WhenLockThrowsException_ReturnsFalse() throws InterruptedException {
        //arrange
        doThrow(new InterruptedException()).when(mockConnectLock).tryLock(10, TimeUnit.SECONDS);

        //act
        keylockDeviceLock.tryLock();

        //assert
        assertFalse(keylockDeviceLock.getIsLocked());
    }

    @Test
    public void unlock_CallsThrough() {
        //arrange

        //act
        keylockDeviceLock.unlock();

        //assert
        verify(mockConnectLock).unlock();
        assertFalse(keylockDeviceLock.getIsLocked());
    }
}
