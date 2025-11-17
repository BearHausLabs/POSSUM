package com.target.devicemanager.components.toneindicator;

import com.target.devicemanager.common.*;
import com.target.devicemanager.components.toneindicator.simulator.SimulatedJposToneIndicator;
import com.target.devicemanager.configuration.ApplicationConfig;
import jpos.ToneIndicator;
import jpos.config.JposEntryRegistry;
import jpos.loader.JposServiceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for ToneIndicator device.
 * Creates and configures beans for ToneIndicator management.
 * Only loads if toneindicator.enabled=true in properties.
 */
@Configuration
@ConditionalOnProperty(name = "toneindicator.enabled", havingValue = "true", matchIfMissing = false)
class ToneIndicatorConfig {
    
    private final SimulatedJposToneIndicator simulatedToneIndicator;
    private final ApplicationConfig applicationConfig;
    
    @Autowired
    ToneIndicatorConfig(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
        this.simulatedToneIndicator = new SimulatedJposToneIndicator();
    }
    
    /**
     * Creates the ToneIndicator device instance.
     * 
     * @return ToneIndicatorDevice configured for real or simulated hardware
     */
    ToneIndicatorDevice getToneIndicator() {
        JposEntryRegistry deviceRegistry = JposServiceLoader.getManager().getEntryRegistry();
        ToneIndicatorDevice device;
        
        if (applicationConfig.IsSimulationMode()) {
            device = new ToneIndicatorDevice(
                    new SimulatedDynamicDevice<>(simulatedToneIndicator, new DevicePower(), 
                                               new DeviceConnector<>(simulatedToneIndicator, deviceRegistry))
            );
        } else {
            ToneIndicator toneIndicator = new ToneIndicator();
            device = new ToneIndicatorDevice(
                    new DynamicDevice<>(toneIndicator, new DevicePower(), 
                                      new DeviceConnector<>(toneIndicator, deviceRegistry))
            );
        }
        
        return device;
    }
    
    /**
     * Creates the ToneIndicatorManager bean.
     * 
     * @return Configured ToneIndicatorManager
     */
    @Bean
    public ToneIndicatorManager getToneIndicatorManager() {
        ToneIndicatorManager toneIndicatorManager = new ToneIndicatorManager(getToneIndicator());
        
        // Register with DeviceAvailabilitySingleton for cross-device access
        DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().setToneIndicatorManager(toneIndicatorManager);
        
        return toneIndicatorManager;
    }
    
    /**
     * Provides the simulated ToneIndicator for testing.
     * 
     * @return SimulatedJposToneIndicator instance
     */
    @Bean
    SimulatedJposToneIndicator getSimulatedToneIndicator() {
        return simulatedToneIndicator;
    }
}

