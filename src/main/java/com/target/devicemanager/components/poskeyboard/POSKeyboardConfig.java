package com.target.devicemanager.components.poskeyboard;

import com.target.devicemanager.common.*;
import com.target.devicemanager.components.poskeyboard.simulator.SimulatedJposPOSKeyboard;
import com.target.devicemanager.configuration.ApplicationConfig;
import jpos.POSKeyboard;
import jpos.config.JposEntryRegistry;
import jpos.loader.JposServiceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Phaser;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Configuration class for POSKeyboard device.
 * Creates and configures beans for POSKeyboard management.
 * Only loads if poskeyboard.enabled=true in properties.
 */
@Configuration
@ConditionalOnProperty(name = "poskeyboard.enabled", havingValue = "true", matchIfMissing = false)
class POSKeyboardConfig {
    
    private final SimulatedJposPOSKeyboard simulatedPOSKeyboard;
    private final ApplicationConfig applicationConfig;
    
    @Autowired
    POSKeyboardConfig(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
        this.simulatedPOSKeyboard = new SimulatedJposPOSKeyboard();
    }
    
    /**
     * Creates the POSKeyboard device instance.
     * 
     * @return POSKeyboardDevice configured for real or simulated hardware
     */
    POSKeyboardDevice getPOSKeyboard() {
        JposEntryRegistry deviceRegistry = JposServiceLoader.getManager().getEntryRegistry();
        POSKeyboardDeviceListener listener = new POSKeyboardDeviceListener(new EventSynchronizer(new Phaser(1)));
        POSKeyboardDevice device;
        
        if (applicationConfig.IsSimulationMode()) {
            device = new POSKeyboardDevice(
                    listener,
                    new SimulatedDynamicDevice<>(simulatedPOSKeyboard, new DevicePower(), 
                                               new DeviceConnector<>(simulatedPOSKeyboard, deviceRegistry))
            );
        } else {
            POSKeyboard posKeyboard = new POSKeyboard();
            device = new POSKeyboardDevice(
                    listener,
                    new DynamicDevice<>(posKeyboard, new DevicePower(), 
                                      new DeviceConnector<>(posKeyboard, deviceRegistry))
            );
        }
        
        // Set the device reference in the listener so it can forward events
        listener.setPOSKeyboardDevice(device);
        
        return device;
    }
    
    /**
     * Creates the POSKeyboardManager bean.
     * 
     * @return Configured POSKeyboardManager
     */
    @Bean
    public POSKeyboardManager getPOSKeyboardManager() {
        POSKeyboardManager posKeyboardManager = new POSKeyboardManager(getPOSKeyboard(), new ReentrantLock());
        
        // Register with DeviceAvailabilitySingleton for cross-device access
        DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().setPOSKeyboardManager(posKeyboardManager);
        
        return posKeyboardManager;
    }
    
    /**
     * Provides the simulated POSKeyboard for testing.
     * 
     * @return SimulatedJposPOSKeyboard instance
     */
    @Bean
    SimulatedJposPOSKeyboard getSimulatedPOSKeyboard() {
        return simulatedPOSKeyboard;
    }
}

