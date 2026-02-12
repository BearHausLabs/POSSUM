package com.target.devicemanager.components.toneindicator;

import com.target.devicemanager.common.*;
import com.target.devicemanager.components.toneindicator.simulator.SimulatedJposToneIndicator;
import com.target.devicemanager.configuration.ApplicationConfig;
import com.target.devicemanager.configuration.WorkstationConfig;
import jpos.ToneIndicator;
import jpos.config.JposEntryRegistry;
import jpos.loader.JposServiceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.Phaser;
import java.util.concurrent.locks.ReentrantLock;

@Configuration
@Profile({"local", "dev", "prod"})
class ToneIndicatorConfig {
    private final SimulatedJposToneIndicator simulatedToneIndicator;
    private final ApplicationConfig applicationConfig;
    private final WorkstationConfig workstationConfig;

    @Autowired
    ToneIndicatorConfig(ApplicationConfig applicationConfig, WorkstationConfig workstationConfig) {
        this.applicationConfig = applicationConfig;
        this.workstationConfig = workstationConfig;
        this.simulatedToneIndicator = new SimulatedJposToneIndicator();
    }

    @Bean
    public ToneIndicatorManager getToneIndicatorManager() {
        DynamicDevice<? extends ToneIndicator> dynamicToneIndicator;
        JposEntryRegistry deviceRegistry = JposServiceLoader.getManager().getEntryRegistry();
        String preferred = workstationConfig.getDeviceLogicalName("toneIndicator");
        boolean autoAdapt = workstationConfig.isAutoAdapt();
        if (applicationConfig.IsSimulationMode()) {
            dynamicToneIndicator = new SimulatedDynamicDevice<>(simulatedToneIndicator, new DevicePower(), new DeviceConnector<>(simulatedToneIndicator, deviceRegistry));
        } else {
            ToneIndicator toneIndicator = new ToneIndicator();
            dynamicToneIndicator = new DynamicDevice<>(toneIndicator, new DevicePower(), new DeviceConnector<>(toneIndicator, deviceRegistry, null, preferred, autoAdapt));
        }

        ToneIndicatorManager toneIndicatorManager = new ToneIndicatorManager(
                new ToneIndicatorDevice(
                        dynamicToneIndicator,
                        new ToneIndicatorDeviceListener(new EventSynchronizer(new Phaser(1)))),
                new ReentrantLock());

        DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().setToneIndicatorManager(toneIndicatorManager);
        return toneIndicatorManager;
    }

    @Bean
    SimulatedJposToneIndicator getSimulatedToneIndicator() {
        return simulatedToneIndicator;
    }
}
