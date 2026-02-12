package com.target.devicemanager.components.msr;

import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.components.msr.entities.CardData;
import jpos.JposConst;
import jpos.JposException;
import jpos.MSR;
import jpos.events.DataEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MSRDeviceTest {

    private MSRDevice msrDevice;
    private MSRDevice msrDeviceLock;

    @Mock
    private MSR mockMSR;
    @Mock
    private DeviceListener mockDeviceListener;
    @Mock
    private DynamicDevice<MSR> mockDynamicMSR;
    @Mock
    private ReentrantLock mockConnectLock;

    @BeforeEach
    public void testInitialize() {
        when(mockDynamicMSR.getDevice()).thenReturn(mockMSR);

        msrDevice = new MSRDevice(mockDeviceListener, mockDynamicMSR);
        msrDeviceLock = new MSRDevice(mockDeviceListener, mockDynamicMSR, mockConnectLock);

        //Default Mock Behavior
        when(mockDynamicMSR.isConnected()).thenReturn(true);
        when(mockDynamicMSR.getDeviceName()).thenReturn("MSR");
    }

    @Test
    public void ctor_WhenDeviceListenerAndDynamicMSRAreNull_ThrowsException() {
        try {
            new MSRDevice(null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("deviceListener cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDeviceListenerIsNull_ThrowsException() {
        try {
            new MSRDevice(null, mockDynamicMSR);
        } catch (IllegalArgumentException iae) {
            assertEquals("deviceListener cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicMSRIsNull_ThrowsException() {
        try {
            new MSRDevice(mockDeviceListener, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("dynamicMSR cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDeviceListenerAndDynamicMSRAreNotNull_DoesNotThrowException() {
        try {
            new MSRDevice(mockDeviceListener, mockDynamicMSR);
        } catch (Exception exception) {
            fail("Existing Device Arguments should not result in an Exception");
        }
    }

    @Test
    public void connect_WhenLockFails() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(false);

        //act
        msrDeviceLock.connect();

        //assert
        verify(mockDynamicMSR, never()).connect();
        verify(mockConnectLock, never()).unlock();
    }

    @Test
    public void connect_WhenConnected() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);
        when(mockDynamicMSR.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);

        //act
        msrDeviceLock.connect();

        //assert
        verify(mockDynamicMSR).connect();
        verify(mockConnectLock).unlock();
        assertTrue(msrDeviceLock.getDeviceConnected());
        verify(mockMSR).addErrorListener(any());
        verify(mockMSR).addDataListener(any());
        verify(mockMSR).addStatusUpdateListener(any());
    }

    @Test
    public void connect_WhenNotConnected() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);
        when(mockDynamicMSR.connect()).thenReturn(DynamicDevice.ConnectionResult.NOT_CONNECTED);

        //act
        msrDeviceLock.connect();

        //assert
        verify(mockDynamicMSR).connect();
        verify(mockConnectLock).unlock();
        assertFalse(msrDeviceLock.getDeviceConnected());
        verify(mockMSR, never()).addErrorListener(any());
        verify(mockMSR, never()).addDataListener(any());
        verify(mockMSR, never()).addStatusUpdateListener(any());
    }

    @Test
    public void connect_WhenAlreadyConnected() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);
        when(mockDynamicMSR.connect()).thenReturn(DynamicDevice.ConnectionResult.ALREADY_CONNECTED);

        //act
        msrDeviceLock.connect();

        //assert
        verify(mockDynamicMSR).connect();
        verify(mockConnectLock).unlock();
        assertTrue(msrDeviceLock.getDeviceConnected());
        verify(mockMSR, never()).addErrorListener(any());
        verify(mockMSR, never()).addDataListener(any());
        verify(mockMSR, never()).addStatusUpdateListener(any());
    }

    @Test
    public void reconnect_WhenTryLockFails() throws InterruptedException, DeviceException {
        //arrange
        doThrow(new InterruptedException()).when(mockConnectLock).tryLock(10, TimeUnit.SECONDS);

        //act
        try {
            msrDeviceLock.reconnect();
        }

        //assert
        catch (DeviceException deviceException) {
            verify(mockDynamicMSR, never()).disconnect();
            verify(mockDynamicMSR, never()).connect();
            verify(mockConnectLock, never()).unlock();
            assertFalse(msrDeviceLock.getDeviceConnected());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void reconnect_WhenAlreadyConnected() throws InterruptedException, DeviceException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);
        when(mockDynamicMSR.connect()).thenReturn(DynamicDevice.ConnectionResult.ALREADY_CONNECTED);
        msrDeviceLock.setDeviceConnected(false);

        //act
        boolean result = msrDeviceLock.reconnect();

        //assert
        verify(mockDynamicMSR, never()).disconnect();
        verify(mockDynamicMSR).connect();
        verify(mockConnectLock).unlock();
        assertTrue(result);
    }

    @Test
    public void reconnect_WhenNotConnected() throws InterruptedException, DeviceException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);
        when(mockDynamicMSR.connect()).thenReturn(DynamicDevice.ConnectionResult.NOT_CONNECTED);
        msrDeviceLock.setDeviceConnected(false);

        //act
        boolean result = msrDeviceLock.reconnect();

        //assert
        verify(mockDynamicMSR, never()).disconnect();
        verify(mockDynamicMSR).connect();
        verify(mockConnectLock).unlock();
        assertFalse(result);
    }

    @Test
    public void reconnect_WhenConnected() throws InterruptedException, DeviceException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);
        when(mockDynamicMSR.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        msrDeviceLock.setDeviceConnected(true);

        //act
        boolean result = msrDeviceLock.reconnect();

        //assert
        verify(mockDynamicMSR).disconnect();
        verify(mockDynamicMSR).connect();
        verify(mockConnectLock).unlock();
        assertTrue(result);
        verify(mockMSR).addErrorListener(any());
        verify(mockMSR).addDataListener(any());
        verify(mockMSR).addStatusUpdateListener(any());
        verify(mockMSR).removeErrorListener(any());
        verify(mockMSR).removeDataListener(any());
        verify(mockMSR).removeStatusUpdateListener(any());
    }

    @Test
    public void getCardData_WhenNotConnected_ThrowsException() throws JposException {
        //arrange
        when(mockDynamicMSR.isConnected()).thenReturn(false);

        //act
        try {
            msrDevice.getCardData();
        }

        //assert
        catch (JposException jposException) {
            verify(mockDeviceListener, never()).startEventListeners();
            verify(mockMSR, never()).setAutoDisable(true);
            verify(mockMSR, never()).setDataEventEnabled(true);
            verify(mockMSR, never()).setDeviceEnabled(true);
            assertEquals(JposConst.JPOS_E_OFFLINE, jposException.getErrorCode());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void getCardData_WhenSetAutoDisable_ThrowsException() throws JposException {
        //arrange
        when(mockDynamicMSR.isConnected()).thenReturn(true).thenReturn(false);
        doThrow(new JposException(JposConst.JPOS_E_NOHARDWARE)).when(mockMSR).setAutoDisable(true);

        //act
        try {
            msrDevice.getCardData();
        }

        //assert
        catch (JposException jposException) {
            verify(mockDeviceListener).startEventListeners();
            verify(mockMSR).setAutoDisable(true);
            verify(mockMSR, never()).setDataEventEnabled(true);
            verify(mockMSR, never()).setDeviceEnabled(true);
            assertEquals(JposConst.JPOS_E_NOHARDWARE, jposException.getErrorCode());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void getCardData_WhenSuccess_ReturnsCardData() throws JposException {
        //arrange
        when(mockDynamicMSR.isConnected()).thenReturn(true);
        byte[] track1 = "TRACK1DATA".getBytes(Charset.defaultCharset());
        byte[] track2 = "TRACK2DATA".getBytes(Charset.defaultCharset());
        byte[] track3 = "TRACK3DATA".getBytes(Charset.defaultCharset());
        byte[] track4 = "TRACK4DATA".getBytes(Charset.defaultCharset());
        when(mockMSR.getTrack1Data()).thenReturn(track1);
        when(mockMSR.getTrack2Data()).thenReturn(track2);
        when(mockMSR.getTrack3Data()).thenReturn(track3);
        when(mockMSR.getTrack4Data()).thenReturn(track4);
        when(mockDeviceListener.waitForData()).thenReturn(new DataEvent(mockMSR, 1));

        //act
        CardData cardData = msrDevice.getCardData();

        //assert
        verify(mockDeviceListener).startEventListeners();
        verify(mockMSR).setAutoDisable(true);
        verify(mockMSR).setDataEventEnabled(true);
        verify(mockMSR).setDeviceEnabled(true);
        verify(mockMSR).getTrack1Data();
        verify(mockMSR).getTrack2Data();
        verify(mockMSR).getTrack3Data();
        verify(mockMSR).getTrack4Data();
        assertEquals("TRACK1DATA", cardData.track1Data);
        assertEquals("TRACK2DATA", cardData.track2Data);
        assertEquals("TRACK3DATA", cardData.track3Data);
        assertEquals("TRACK4DATA", cardData.track4Data);
    }

    @Test
    public void getCardData_WhenWaitForDataThrows_ThrowsException() throws JposException {
        //arrange
        when(mockDynamicMSR.isConnected()).thenReturn(true);
        when(mockDeviceListener.waitForData()).thenThrow(new JposException(JposConst.JPOS_E_TIMEOUT));

        //act
        try {
            msrDevice.getCardData();
        }

        //assert
        catch (JposException jposException) {
            verify(mockDeviceListener).startEventListeners();
            assertEquals(JposConst.JPOS_E_TIMEOUT, jposException.getErrorCode());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void getCardData_WhenDataEventSourceNotMSR_ThrowsException() throws JposException {
        //arrange
        when(mockDynamicMSR.isConnected()).thenReturn(true);
        when(mockDeviceListener.waitForData()).thenReturn(new DataEvent(new Object(), 1));

        //act
        try {
            msrDevice.getCardData();
        }

        //assert
        catch (JposException jposException) {
            assertEquals(JposConst.JPOS_E_FAILURE, jposException.getErrorCode());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void getCardData_WhenGetTrackDataThrows_ThrowsException() throws JposException {
        //arrange
        when(mockDynamicMSR.isConnected()).thenReturn(true);
        when(mockDeviceListener.waitForData()).thenReturn(new DataEvent(mockMSR, 1));
        when(mockMSR.getTrack1Data()).thenThrow(new JposException(JposConst.JPOS_E_EXTENDED));

        //act
        try {
            msrDevice.getCardData();
        }

        //assert
        catch (JposException jposException) {
            assertEquals(JposConst.JPOS_E_EXTENDED, jposException.getErrorCode());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void cancelCardData_WhenSetDeviceEnabledFails_ThrowsException() throws JposException {
        //arrange
        when(mockDynamicMSR.isConnected()).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockMSR).setDeviceEnabled(false);

        //act
        msrDevice.cancelCardData();

        //assert
        verify(mockMSR).setDeviceEnabled(false);
        verify(mockDeviceListener).stopWaitingForData();
    }

    @Test
    public void cancelCardData_WhenSetDeviceEnabledSucceeds() throws JposException {
        //arrange
        when(mockDynamicMSR.isConnected()).thenReturn(true);

        //act
        msrDevice.cancelCardData();

        //assert
        verify(mockMSR).setDeviceEnabled(false);
        verify(mockDeviceListener).stopWaitingForData();
    }

    @Test
    public void getDeviceName_Returns() {
        //arrange
        String expected = "MSR";

        //act
        String actual = msrDevice.getDeviceName();

        //assert
        assertEquals(expected, actual);
    }

    @Test
    public void isConnected_ReturnsTrue() {
        //arrange
        when(mockDynamicMSR.isConnected()).thenReturn(true);

        //act
        boolean actual = msrDevice.isConnected();

        //assert
        assertTrue(actual);
    }

    @Test
    public void isConnected_ReturnsFalse() {
        //arrange
        when(mockDynamicMSR.isConnected()).thenReturn(false);

        //act
        boolean actual = msrDevice.isConnected();

        //assert
        assertFalse(actual);
    }
}
