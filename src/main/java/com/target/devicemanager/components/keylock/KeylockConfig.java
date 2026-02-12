package com.target.devicemanager.components.keylock;

import com.target.devicemanager.common.*;
import com.target.devicemanager.components.keylock.simulator.SimulatedJposKeylock;
import com.target.devicemanager.configuration.ApplicationConfig;
import jpos.Keylock;
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
class KeylockConfig {
    private final SimulatedJposKeylock simulatedKeylock;
    private final ApplicationConfig applicationConfig;

    @Autowired
    KeylockConfig(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
        this.simulatedKeylock = new SimulatedJposKeylock();
    }

    @Bean
    public KeylockManager getKeylockManager() {
        DynamicDevice<? extends Keylock> dynamicKeylock;
        JposEntryRegistry deviceRegistry = JposServiceLoader.getManager().getEntryRegistry();
        if (applicationConfig.IsSimulationMode()) {
            dynamicKeylock = new SimulatedDynamicDevice<>(simulatedKeylock, new DevicePower(), new DeviceConnector<>(simulatedKeylock, deviceRegistry));

        } else {
            Keylock keylock = new Keylock();
            dynamicKeylock = new DynamicDevice<>(keylock, new DevicePower(), new DeviceConnector<>(keylock, deviceRegistry));
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
