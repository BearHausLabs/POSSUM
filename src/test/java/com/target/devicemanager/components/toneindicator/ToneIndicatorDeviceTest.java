package com.target.devicemanager.components.toneindicator;

import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.components.toneindicator.entities.ToneRequest;
import jpos.JposConst;
import jpos.JposException;
import jpos.ToneIndicator;
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
public class ToneIndicatorDeviceTest {

    private ToneIndicatorDevice toneIndicatorDevice;
    private ToneIndicatorDevice toneIndicatorDeviceLock;

    @Mock
    private DynamicDevice<ToneIndicator> mockDynamicToneIndicator;
    @Mock
    private ToneIndicator mockToneIndicator;
    @Mock
    private StatusUpdateEvent mockStatusUpdateEvent;
    @Mock
    private DeviceListener mockDeviceListener;
    @Mock
    private ReentrantLock mockConnectLock;

    @BeforeEach
    public void testInitialize() throws Exception {
        when(mockDynamicToneIndicator.getDevice()).thenReturn(mockToneIndicator);

        toneIndicatorDevice = new ToneIndicatorDevice(mockDynamicToneIndicator, mockDeviceListener);
        toneIndicatorDeviceLock = new ToneIndicatorDevice(mockDynamicToneIndicator, mockDeviceListener, mockConnectLock);
    }

    @Test
    public void ctor_WhenDynamicToneIndicatorAndDeviceListenerAreNull_ThrowsException() {
        try {
            new ToneIndicatorDevice(null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("dynamicToneIndicator cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicToneIndicatorIsNull_ThrowsException() {
        try {
            new ToneIndicatorDevice(null, mockDeviceListener);
        } catch (IllegalArgumentException iae) {
            assertEquals("dynamicToneIndicator cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDeviceListenerIsNull_ThrowsException() {
        try {
            new ToneIndicatorDevice(mockDynamicToneIndicator, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("deviceListener cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicToneIndicatorAndDeviceListenerAreNotNull_DoesNotThrowException() {
        try {
            new ToneIndicatorDevice(mockDynamicToneIndicator, mockDeviceListener);
        } catch (Exception exception) {
            fail("Existing Device Argument should not result in an Exception");
        }
    }

    @Test
    public void connect_CallsDynamicConnect() {
        //arrange

        //act
        assertTrue(toneIndicatorDevice.connect());

        //assert
        verify(mockDynamicToneIndicator).connect();
        verify(mockDynamicToneIndicator, times(2)).getDevice();
        assertTrue(toneIndicatorDevice.isConnected());
    }

    @Test
    public void connect_CallsDynamicConnect_ReturnsNotConnected() {
        //arrange
        when(mockDynamicToneIndicator.connect()).thenReturn(DynamicDevice.ConnectionResult.NOT_CONNECTED);

        //act
        assertFalse(toneIndicatorDevice.connect());

        //assert
        verify(mockDynamicToneIndicator).connect();
        verify(mockDynamicToneIndicator, never()).getDevice();
        assertFalse(toneIndicatorDevice.isConnected());
    }

    @Test
    public void connect_WhenAreListenersAttachedFalse_AttachListeners() {
        //arrange
        toneIndicatorDevice.setAreListenersAttached(false);

        //act
        assertTrue(toneIndicatorDevice.connect());

        //assert
        verify(mockDynamicToneIndicator).connect();
        verify(mockToneIndicator).addStatusUpdateListener(any());
        assertTrue(toneIndicatorDevice.getAreListenersAttached());
    }

    @Test
    public void connect_WhenAreListenersAttachedTrue_DoesNotAttachListeners() {
        //arrange
        toneIndicatorDevice.setAreListenersAttached(true);

        //act
        assertTrue(toneIndicatorDevice.connect());

        //assert
        verify(mockDynamicToneIndicator).connect();
        verify(mockToneIndicator, never()).addStatusUpdateListener(any());
        assertTrue(toneIndicatorDevice.getAreListenersAttached());
    }

    @Test
    public void connect_WhenGetDeviceEnabledFalse_SetDeviceEnabled() throws JposException {
        //arrange
        when(mockToneIndicator.getDeviceEnabled()).thenReturn(false);

        //act
        assertTrue(toneIndicatorDevice.connect());

        //assert
        verify(mockDynamicToneIndicator).connect();
        verify(mockToneIndicator).setDeviceEnabled(true);
        assertTrue(toneIndicatorDevice.isConnected());
    }

    @Test
    public void connect_WhenGetDeviceEnabledTrue_DoesNotSetDeviceEnabled() throws JposException {
        //arrange
        when(mockToneIndicator.getDeviceEnabled()).thenReturn(true);

        //act
        assertTrue(toneIndicatorDevice.connect());

        //assert
        verify(mockDynamicToneIndicator).connect();
        verify(mockToneIndicator, never()).setDeviceEnabled(true);
        assertFalse(toneIndicatorDevice.isConnected());
    }

    @Test
    public void connect_WhenGetDeviceEnabledThrowsException() throws JposException {
        //arrange
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockToneIndicator).getDeviceEnabled();

        //act
        assertFalse(toneIndicatorDevice.connect());

        //assert
        verify(mockDynamicToneIndicator).connect();
        assertFalse(toneIndicatorDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedTrue() throws JposException {
        //arrange
        when(mockDynamicToneIndicator.isConnected()).thenReturn(true);

        //act
        toneIndicatorDevice.disconnect();

        //assert
        assertFalse(toneIndicatorDevice.getAreListenersAttached());
        verify(mockToneIndicator).getDeviceEnabled();
    }

    @Test
    public void disconnect_WhenIsConnectedTrue_WhenGetDeviceEnabledTrue_SetDeviceEnabled() throws JposException {
        //arrange
        when(mockDynamicToneIndicator.isConnected()).thenReturn(true);
        when(mockToneIndicator.getDeviceEnabled()).thenReturn(true);

        //act
        toneIndicatorDevice.disconnect();

        //assert
        verify(mockToneIndicator).setDeviceEnabled(false);
        verify(mockDynamicToneIndicator).disconnect();
        assertFalse(toneIndicatorDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedFalse() throws JposException {
        //arrange
        when(mockDynamicToneIndicator.isConnected()).thenReturn(false);

        //act
        toneIndicatorDevice.disconnect();

        //assert
        verify(mockToneIndicator, never()).getDeviceEnabled();
        verify(mockDynamicToneIndicator, never()).disconnect();
    }

    @Test
    public void playSound_WhenIsConnectedFalse_ThrowsException() {
        //arrange
        toneIndicatorDevice.setDeviceConnected(false);
        ToneRequest toneRequest = new ToneRequest(1500, 100, 50);

        //act
        try {
            toneIndicatorDevice.playSound(toneRequest);
        }

        //assert
        catch (JposException jposException) {
            assertEquals(JposConst.JPOS_E_OFFLINE, jposException.getErrorCode());
            return;
        } catch (DeviceException deviceException) {
            fail("Expected JposException, got DeviceException");
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void playSound_WhenIsConnectedTrue_CallsSound() throws JposException, DeviceException {
        //arrange
        toneIndicatorDevice.setDeviceConnected(true);
        ToneRequest toneRequest = new ToneRequest(1500, 100, 50);

        //act
        toneIndicatorDevice.playSound(toneRequest);

        //assert
        verify(mockDeviceListener).startEventListeners();
        verify(mockToneIndicator).setTone1Pitch(1500);
        verify(mockToneIndicator).setTone1Duration(100);
        verify(mockToneIndicator).setTone1Volume(50);
        verify(mockToneIndicator).sound(1, 0);
    }

    @Test
    public void playSound_WithTwoTones_CallsSoundWithBothTones() throws JposException, DeviceException {
        //arrange
        toneIndicatorDevice.setDeviceConnected(true);
        ToneRequest toneRequest = new ToneRequest(1500, 100, 50, 2000, 200, 75, 50);

        //act
        toneIndicatorDevice.playSound(toneRequest);

        //assert
        verify(mockToneIndicator).setTone1Pitch(1500);
        verify(mockToneIndicator).setTone1Duration(100);
        verify(mockToneIndicator).setTone1Volume(50);
        verify(mockToneIndicator).setTone2Pitch(2000);
        verify(mockToneIndicator).setTone2Duration(200);
        verify(mockToneIndicator).setTone2Volume(75);
        verify(mockToneIndicator).setInterToneWait(50);
        verify(mockToneIndicator).sound(2, 0);
    }

    @Test
    public void playSound_WhenSoundThrowsJposException_ThrowsException() throws JposException {
        //arrange
        toneIndicatorDevice.setDeviceConnected(true);
        ToneRequest toneRequest = new ToneRequest(1500, 100, 50);
        doThrow(new JposException(JposConst.JPOS_E_FAILURE)).when(mockToneIndicator).sound(1, 0);

        //act
        try {
            toneIndicatorDevice.playSound(toneRequest);
        }

        //assert
        catch (JposException jposException) {
            assertEquals(JposConst.JPOS_E_FAILURE, jposException.getErrorCode());
            return;
        } catch (DeviceException deviceException) {
            fail("Expected JposException, got DeviceException");
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void getDeviceName_ReturnsName() {
        //arrange
        String expectedDeviceName = "toneIndicator";
        when(mockDynamicToneIndicator.getDeviceName()).thenReturn(expectedDeviceName);

        //act
        String actual = toneIndicatorDevice.getDeviceName();

        //assert
        assertEquals(expectedDeviceName, actual);
    }

    @Test
    public void isConnected_ReturnsTrueFromDynamicDevice() {
        //arrange
        toneIndicatorDevice.setDeviceConnected(true);

        //act
        boolean actual = toneIndicatorDevice.isConnected();

        //assert
        assertTrue(actual);
    }

    @Test
    public void isConnected_ReturnsFalseFromDynamicDevice() {
        //arrange
        toneIndicatorDevice.setDeviceConnected(false);

        //act
        boolean actual = toneIndicatorDevice.isConnected();

        //assert
        assertFalse(actual);
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOff_Disconnected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFF);
        ToneIndicatorDevice spy = spy(toneIndicatorDevice);

        //act
        spy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(spy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOffOffline_Disconnected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFF_OFFLINE);
        ToneIndicatorDevice spy = spy(toneIndicatorDevice);

        //act
        spy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(spy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOffline_Disconnected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFFLINE);
        ToneIndicatorDevice spy = spy(toneIndicatorDevice);

        //act
        spy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(spy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOnline_Connected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_ONLINE);
        ToneIndicatorDevice spy = spy(toneIndicatorDevice);

        //act
        spy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertTrue(spy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenOtherStatus_DoNothing() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(572);
        ToneIndicatorDevice spy = spy(toneIndicatorDevice);

        //act
        spy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        //do nothing
    }

    @Test
    public void tryLock_WhenLock_ReturnsTrue() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);

        //act
        toneIndicatorDeviceLock.tryLock();

        //assert
        assertTrue(toneIndicatorDeviceLock.getIsLocked());
    }

    @Test
    public void tryLock_WhenNotLock_ReturnsFalse() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(false);

        //act
        toneIndicatorDeviceLock.tryLock();

        //assert
        assertFalse(toneIndicatorDeviceLock.getIsLocked());
    }

    @Test
    public void tryLock_WhenLockThrowsException_ReturnsFalse() throws InterruptedException {
        //arrange
        doThrow(new InterruptedException()).when(mockConnectLock).tryLock(10, TimeUnit.SECONDS);

        //act
        toneIndicatorDeviceLock.tryLock();

        //assert
        assertFalse(toneIndicatorDeviceLock.getIsLocked());
    }

    @Test
    public void unlock_CallsThrough() {
        //arrange

        //act
        toneIndicatorDeviceLock.unlock();

        //assert
        verify(mockConnectLock).unlock();
        assertFalse(toneIndicatorDeviceLock.getIsLocked());
    }
}
