package com.target.devicemanager.components.keylock;

import com.target.devicemanager.common.*;
import com.target.devicemanager.components.keylock.simulator.SimulatedJposKeylock;
import com.target.devicemanager.configuration.ApplicationConfig;
import jpos.Keylock;
import jpos.config.JposEntryRegistry;
import jpos.loader.JposServiceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for KeyLock device.
 * Creates and configures beans for KeyLock management.
 * Only loads if keylock.enabled=true in properties.
 */
@Configuration
@ConditionalOnProperty(name = "keylock.enabled", havingValue = "true", matchIfMissing = false)
class KeyLockConfig {
    
    private final SimulatedJposKeylock simulatedKeylock;
    private final ApplicationConfig applicationConfig;
    
    @Autowired
    KeyLockConfig(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
        this.simulatedKeylock = new SimulatedJposKeylock();
    }
    
    /**
     * Creates the KeyLock device instance.
     * 
     * @return KeyLockDevice configured for real or simulated hardware
     */
    KeyLockDevice getKeyLock() {
        JposEntryRegistry deviceRegistry = JposServiceLoader.getManager().getEntryRegistry();
        KeyLockDevice device;
        
        if (applicationConfig.IsSimulationMode()) {
            device = new KeyLockDevice(
                    new SimulatedDynamicDevice<>(simulatedKeylock, new DevicePower(), 
                                               new DeviceConnector<>(simulatedKeylock, deviceRegistry))
            );
        } else {
            Keylock keylock = new Keylock();
            device = new KeyLockDevice(
                    new DynamicDevice<>(keylock, new DevicePower(), 
                                      new DeviceConnector<>(keylock, deviceRegistry))
            );
        }
        
        return device;
    }
    
    /**
     * Creates the KeyLockManager bean.
     * 
     * @return Configured KeyLockManager
     */
    @Bean
    public KeyLockManager getKeyLockManager() {
        KeyLockManager keyLockManager = new KeyLockManager(getKeyLock());
        
        // Register with DeviceAvailabilitySingleton for cross-device access
        DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().setKeyLockManager(keyLockManager);
        
        return keyLockManager;
    }
    
    /**
     * Provides the simulated KeyLock for testing.
     * 
     * @return SimulatedJposKeylock instance
     */
    @Bean
    SimulatedJposKeylock getSimulatedKeylock() {
        return simulatedKeylock;
    }
}

