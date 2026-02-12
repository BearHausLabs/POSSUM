package com.target.devicemanager.components.poskeyboard;

import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.components.poskeyboard.entities.KeyboardEventData;
import jpos.POSKeyboard;
import jpos.POSKeyboardConst;
import jpos.JposConst;
import jpos.JposException;
import jpos.events.DataEvent;
import jpos.events.StatusUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class POSKeyboardDeviceTest {

    private POSKeyboardDevice posKeyboardDevice;
    private POSKeyboardDevice posKeyboardDeviceLock;

    @Mock
    private DynamicDevice<POSKeyboard> mockDynamicKeyboard;
    @Mock
    private POSKeyboard mockKeyboard;
    @Mock
    private StatusUpdateEvent mockStatusUpdateEvent;
    @Mock
    private DataEvent mockDataEvent;
    @Mock
    private DeviceListener mockDeviceListener;
    @Mock
    private ReentrantLock mockConnectLock;

    @BeforeEach
    public void testInitialize() throws Exception {
        when(mockDynamicKeyboard.getDevice()).thenReturn(mockKeyboard);

        posKeyboardDevice = new POSKeyboardDevice(mockDynamicKeyboard, mockDeviceListener);
        posKeyboardDeviceLock = new POSKeyboardDevice(mockDynamicKeyboard, mockDeviceListener, mockConnectLock);
    }

    @Test
    public void ctor_WhenDynamicKeyboardAndDeviceListenerAreNull_ThrowsException() {
        try {
            new POSKeyboardDevice(null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("dynamicKeyboard cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicKeyboardIsNull_ThrowsException() {
        try {
            new POSKeyboardDevice(null, mockDeviceListener);
        } catch (IllegalArgumentException iae) {
            assertEquals("dynamicKeyboard cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDeviceListenerIsNull_ThrowsException() {
        try {
            new POSKeyboardDevice(mockDynamicKeyboard, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("deviceListener cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicKeyboardAndDeviceListenerAreNotNull_DoesNotThrowException() {
        try {
            new POSKeyboardDevice(mockDynamicKeyboard, mockDeviceListener);
        } catch (Exception exception) {
            fail("Existing Device Argument should not result in an Exception");
        }
    }

    @Test
    public void connect_CallsDynamicConnect() {
        //act
        assertTrue(posKeyboardDevice.connect());

        //assert
        verify(mockDynamicKeyboard).connect();
        verify(mockDynamicKeyboard, times(2)).getDevice();
        assertTrue(posKeyboardDevice.isConnected());
    }

    @Test
    public void connect_CallsDynamicConnect_ReturnsNotConnected() {
        //arrange
        when(mockDynamicKeyboard.connect()).thenReturn(DynamicDevice.ConnectionResult.NOT_CONNECTED);

        //act
        assertFalse(posKeyboardDevice.connect());

        //assert
        verify(mockDynamicKeyboard).connect();
        verify(mockDynamicKeyboard, never()).getDevice();
        assertFalse(posKeyboardDevice.isConnected());
    }

    @Test
    public void connect_WhenAreListenersAttachedFalse_AttachListeners() {
        //arrange
        posKeyboardDevice.setAreListenersAttached(false);

        //act
        assertTrue(posKeyboardDevice.connect());

        //assert
        verify(mockDynamicKeyboard).connect();
        verify(mockKeyboard).addDataListener(any());
        verify(mockKeyboard).addStatusUpdateListener(any());
        assertTrue(posKeyboardDevice.getAreListenersAttached());
        assertTrue(posKeyboardDevice.isConnected());
    }

    @Test
    public void connect_WhenAreListenersAttachedTrue_DoesNotAttachListeners() {
        //arrange
        posKeyboardDevice.setAreListenersAttached(true);

        //act
        assertTrue(posKeyboardDevice.connect());

        //assert
        verify(mockDynamicKeyboard).connect();
        verify(mockKeyboard, never()).addDataListener(any());
        verify(mockKeyboard, never()).addStatusUpdateListener(any());
        assertTrue(posKeyboardDevice.getAreListenersAttached());
        assertTrue(posKeyboardDevice.isConnected());
    }

    @Test
    public void connect_WhenGetDeviceEnabledFalse_SetDeviceEnabled() throws JposException {
        //arrange
        when(mockKeyboard.getDeviceEnabled()).thenReturn(false);

        //act
        assertTrue(posKeyboardDevice.connect());

        //assert
        verify(mockKeyboard).setDeviceEnabled(true);
        verify(mockKeyboard).setDataEventEnabled(true);
        assertTrue(posKeyboardDevice.isConnected());
    }

    @Test
    public void connect_WhenGetDeviceEnabledTrue_DoesNotSetDeviceEnabled() throws JposException {
        //arrange
        when(mockKeyboard.getDeviceEnabled()).thenReturn(true);

        //act
        assertTrue(posKeyboardDevice.connect());

        //assert
        verify(mockKeyboard, never()).setDeviceEnabled(true);
        verify(mockKeyboard, never()).setDataEventEnabled(true);
        assertFalse(posKeyboardDevice.isConnected());
    }

    @Test
    public void connect_WhenGetDeviceEnabledThrowsException() throws JposException {
        //arrange
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockKeyboard).getDeviceEnabled();

        //act
        assertFalse(posKeyboardDevice.connect());

        //assert
        verify(mockKeyboard, never()).setDeviceEnabled(true);
        assertFalse(posKeyboardDevice.isConnected());
    }

    @Test
    public void connect_WhenSetDeviceEnabledThrowsException() throws JposException {
        //arrange
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockKeyboard).setDeviceEnabled(true);

        //act
        assertFalse(posKeyboardDevice.connect());

        //assert
        verify(mockKeyboard).setDeviceEnabled(true);
        assertFalse(posKeyboardDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedTrue() throws JposException {
        //arrange
        when(mockDynamicKeyboard.isConnected()).thenReturn(true);

        //act
        posKeyboardDevice.disconnect();

        //assert
        assertFalse(posKeyboardDevice.getAreListenersAttached());
    }

    @Test
    public void disconnect_WhenIsConnectedTrue_WhenAreListenersAttachedTrue_DetachListeners() throws JposException {
        //arrange
        when(mockDynamicKeyboard.isConnected()).thenReturn(true);
        posKeyboardDevice.setAreListenersAttached(true);

        //act
        posKeyboardDevice.disconnect();

        //assert
        assertFalse(posKeyboardDevice.getAreListenersAttached());
    }

    @Test
    public void disconnect_WhenIsConnectedFalse() throws JposException {
        //arrange
        when(mockDynamicKeyboard.isConnected()).thenReturn(false);
        posKeyboardDevice.setAreListenersAttached(true);

        //act
        posKeyboardDevice.disconnect();

        //assert
        assertTrue(posKeyboardDevice.getAreListenersAttached());
    }

    @Test
    public void dataOccurred_ReadsKeyDataAndCallsCallback() throws JposException {
        //arrange
        when(mockKeyboard.getPOSKeyData()).thenReturn(42);
        when(mockKeyboard.getPOSKeyEventType()).thenReturn(POSKeyboardConst.KBD_KET_KEYDOWN);
        AtomicReference<KeyboardEventData> receivedEvent = new AtomicReference<>();
        posKeyboardDevice.setEventCallback(receivedEvent::set);

        //act
        posKeyboardDevice.dataOccurred(mockDataEvent);

        //assert
        assertNotNull(receivedEvent.get());
        assertEquals(42, receivedEvent.get().getKeyCode());
        assertEquals("KEY_DOWN", receivedEvent.get().getEventType());
        verify(mockKeyboard).setDataEventEnabled(true);
    }

    @Test
    public void dataOccurred_WhenKeyUp_SetsCorrectEventType() throws JposException {
        //arrange
        when(mockKeyboard.getPOSKeyData()).thenReturn(10);
        when(mockKeyboard.getPOSKeyEventType()).thenReturn(POSKeyboardConst.KBD_KET_KEYUP);
        AtomicReference<KeyboardEventData> receivedEvent = new AtomicReference<>();
        posKeyboardDevice.setEventCallback(receivedEvent::set);

        //act
        posKeyboardDevice.dataOccurred(mockDataEvent);

        //assert
        assertNotNull(receivedEvent.get());
        assertEquals(10, receivedEvent.get().getKeyCode());
        assertEquals("KEY_UP", receivedEvent.get().getEventType());
        verify(mockKeyboard).setDataEventEnabled(true);
    }

    @Test
    public void dataOccurred_WhenNoCallback_DoesNotThrow() throws JposException {
        //arrange
        when(mockKeyboard.getPOSKeyData()).thenReturn(1);
        when(mockKeyboard.getPOSKeyEventType()).thenReturn(POSKeyboardConst.KBD_KET_KEYDOWN);

        //act - should not throw even without a callback set
        posKeyboardDevice.dataOccurred(mockDataEvent);

        //assert
        verify(mockKeyboard).setDataEventEnabled(true);
    }

    @Test
    public void dataOccurred_WhenGetPOSKeyDataThrowsException_ReEnablesDataEvents() throws JposException {
        //arrange
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockKeyboard).getPOSKeyData();

        //act
        posKeyboardDevice.dataOccurred(mockDataEvent);

        //assert
        verify(mockKeyboard, times(1)).setDataEventEnabled(true);
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOff_Disconnected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFF);
        POSKeyboardDevice spy = spy(posKeyboardDevice);

        //act
        spy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(spy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOffOffline_Disconnected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFF_OFFLINE);
        POSKeyboardDevice spy = spy(posKeyboardDevice);

        //act
        spy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(spy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOffline_Disconnected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFFLINE);
        POSKeyboardDevice spy = spy(posKeyboardDevice);

        //act
        spy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(spy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOnline_Connected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_ONLINE);
        POSKeyboardDevice spy = spy(posKeyboardDevice);

        //act
        spy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertTrue(spy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenOtherStatus_DoNothing() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(572);
        POSKeyboardDevice spy = spy(posKeyboardDevice);

        //act
        spy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert - no state change expected
    }

    @Test
    public void getDeviceName_ReturnsName() {
        //arrange
        String expectedDeviceName = "POSKeyboard";
        when(mockDynamicKeyboard.getDeviceName()).thenReturn(expectedDeviceName);

        //act
        String actual = posKeyboardDevice.getDeviceName();

        //assert
        assertEquals(expectedDeviceName, actual);
    }

    @Test
    public void isConnected_ReturnsTrueWhenSet() {
        //arrange
        posKeyboardDevice.setDeviceConnected(true);

        //act/assert
        assertTrue(posKeyboardDevice.isConnected());
    }

    @Test
    public void isConnected_ReturnsFalseWhenSet() {
        //arrange
        posKeyboardDevice.setDeviceConnected(false);

        //act/assert
        assertFalse(posKeyboardDevice.isConnected());
    }

    @Test
    public void tryLock_WhenLock_ReturnsTrue() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);

        //act
        posKeyboardDeviceLock.tryLock();

        //assert
        assertTrue(posKeyboardDeviceLock.getIsLocked());
    }

    @Test
    public void tryLock_WhenNotLock_ReturnsFalse() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(false);

        //act
        posKeyboardDeviceLock.tryLock();

        //assert
        assertFalse(posKeyboardDeviceLock.getIsLocked());
    }

    @Test
    public void tryLock_WhenLockThrowsException_ReturnsFalse() throws InterruptedException {
        //arrange
        doThrow(new InterruptedException()).when(mockConnectLock).tryLock(10, TimeUnit.SECONDS);

        //act
        posKeyboardDeviceLock.tryLock();

        //assert
        assertFalse(posKeyboardDeviceLock.getIsLocked());
    }

    @Test
    public void unlock_CallsThrough() {
        //act
        posKeyboardDeviceLock.unlock();

        //assert
        verify(mockConnectLock).unlock();
        assertFalse(posKeyboardDeviceLock.getIsLocked());
    }
}
