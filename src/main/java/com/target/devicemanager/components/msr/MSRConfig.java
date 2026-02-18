package com.target.devicemanager.components.msr;

import com.target.devicemanager.common.*;
import com.target.devicemanager.components.msr.simulator.SimulatedJposMSR;
import com.target.devicemanager.configuration.ApplicationConfig;
import com.target.devicemanager.configuration.WorkstationConfig;
import jpos.MSR;
import jpos.config.JposEntryRegistry;
import jpos.loader.JposServiceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.concurrent.Phaser;
import java.util.concurrent.locks.ReentrantLock;

@Configuration
@ConditionalOnProperty(name = "possum.device.msr.enabled", havingValue = "true")
class MSRConfig {
    private final SimulatedJposMSR simulatedMSR;
    private final ApplicationConfig applicationConfig;
    private final WorkstationConfig workstationConfig;
    private final Environment environment;

    @Autowired
    MSRConfig(ApplicationConfig applicationConfig, WorkstationConfig workstationConfig, Environment environment) {
        this.applicationConfig = applicationConfig;
        this.workstationConfig = workstationConfig;
        this.environment = environment;
        this.simulatedMSR = new SimulatedJposMSR();
    }

    @Bean
    public MSRManager getMSRManager() {
        DynamicDevice<? extends MSR> dynamicMSR;
        JposEntryRegistry deviceRegistry = JposServiceLoader.getManager().getEntryRegistry();
        String preferred = environment.getProperty("possum.device.msr.logicalName");
        if (preferred == null) {
            preferred = workstationConfig.getDeviceLogicalName("msr");
        }
        boolean autoAdapt = workstationConfig.isAutoAdapt();

        if (applicationConfig.IsSimulationMode()) {
            dynamicMSR = new SimulatedDynamicDevice<>(simulatedMSR, new DevicePower(), new DeviceConnector<>(simulatedMSR, deviceRegistry));
        } else {
            MSR msr = new MSR();
            dynamicMSR = new DynamicDevice<>(msr, new DevicePower(), new DeviceConnector<>(msr, deviceRegistry, null, preferred, autoAdapt));
        }

        MSRManager msrManager = new MSRManager(
                new MSRDevice(
                        new MSRDeviceListener(new EventSynchronizer(new Phaser(1))),
                        dynamicMSR),
                new ReentrantLock());

        DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().setMsrManager(msrManager);
        return msrManager;
    }

    @Bean
    SimulatedJposMSR getSimulatedMSR() {
        return simulatedMSR;
    }
}
