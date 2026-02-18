package com.target.devicemanager.components.keylock;

import com.target.devicemanager.common.*;
import com.target.devicemanager.components.keylock.simulator.SimulatedJposKeylock;
import com.target.devicemanager.configuration.ApplicationConfig;
import com.target.devicemanager.configuration.WorkstationConfig;
import jpos.Keylock;
import jpos.config.JposEntryRegistry;
import jpos.loader.JposServiceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.concurrent.Phaser;
import java.util.concurrent.locks.ReentrantLock;

@Configuration
@Profile({"local", "dev", "prod"})
@ConditionalOnProperty(name = "possum.device.keylock.enabled", havingValue = "true")
class KeylockConfig {
    private final SimulatedJposKeylock simulatedKeylock;
    private final ApplicationConfig applicationConfig;
    private final WorkstationConfig workstationConfig;
    private final Environment environment;

    @Autowired
    KeylockConfig(ApplicationConfig applicationConfig, WorkstationConfig workstationConfig, Environment environment) {
        this.applicationConfig = applicationConfig;
        this.workstationConfig = workstationConfig;
        this.simulatedKeylock = new SimulatedJposKeylock();
        this.environment = environment;
    }

    @Bean
    public KeylockManager getKeylockManager() {
        DynamicDevice<? extends Keylock> dynamicKeylock;
        JposEntryRegistry deviceRegistry = JposServiceLoader.getManager().getEntryRegistry();
        String preferred = environment.getProperty("possum.device.keylock.logicalName");
        if (preferred == null) {
            preferred = workstationConfig.getDeviceLogicalName("keylock");
        }
        boolean autoAdapt = workstationConfig.isAutoAdapt();
        if (applicationConfig.IsSimulationMode()) {
            dynamicKeylock = new SimulatedDynamicDevice<>(simulatedKeylock, new DevicePower(), new DeviceConnector<>(simulatedKeylock, deviceRegistry));

        } else {
            Keylock keylock = new Keylock();
            dynamicKeylock = new DynamicDevice<>(keylock, new DevicePower(), new DeviceConnector<>(keylock, deviceRegistry, null, preferred, autoAdapt, true));
        }

        KeylockManager keylockManager = new KeylockManager(
                new KeylockDevice(
                        dynamicKeylock,
                        new KeylockDeviceListener(new EventSynchronizer(new Phaser(1)))),
                new ReentrantLock());

        DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().setKeylockManager(keylockManager);
        return keylockManager;
    }

    @Bean
    SimulatedJposKeylock getSimulatedKeylock() {
        return simulatedKeylock;
    }

}
