package com.target.devicemanager.components.poskeyboard;

import com.target.devicemanager.common.*;
import com.target.devicemanager.components.poskeyboard.simulator.SimulatedJposPOSKeyboard;
import com.target.devicemanager.configuration.ApplicationConfig;
import com.target.devicemanager.configuration.WorkstationConfig;
import jpos.POSKeyboard;
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
@ConditionalOnProperty(name = "possum.device.posKeyboard.enabled", havingValue = "true")
class POSKeyboardConfig {
    private final SimulatedJposPOSKeyboard simulatedPOSKeyboard;
    private final ApplicationConfig applicationConfig;
    private final WorkstationConfig workstationConfig;
    private final Environment environment;

    @Autowired
    POSKeyboardConfig(ApplicationConfig applicationConfig, WorkstationConfig workstationConfig, Environment environment) {
        this.applicationConfig = applicationConfig;
        this.workstationConfig = workstationConfig;
        this.environment = environment;
        this.simulatedPOSKeyboard = new SimulatedJposPOSKeyboard();
    }

    @Bean
    public POSKeyboardManager getPosKeyboardManager() {
        DynamicDevice<? extends POSKeyboard> dynamicKeyboard;
        JposEntryRegistry deviceRegistry = JposServiceLoader.getManager().getEntryRegistry();
        String preferred = environment.getProperty("possum.device.posKeyboard.logicalName");
        if (preferred == null) {
            preferred = workstationConfig.getDeviceLogicalName("posKeyboard");
        }
        boolean autoAdapt = workstationConfig.isAutoAdapt();
        if (applicationConfig.IsSimulationMode()) {
            dynamicKeyboard = new SimulatedDynamicDevice<>(simulatedPOSKeyboard, new DevicePower(), new DeviceConnector<>(simulatedPOSKeyboard, deviceRegistry));
        } else {
            POSKeyboard keyboard = new POSKeyboard();
            dynamicKeyboard = new DynamicDevice<>(keyboard, new DevicePower(), new DeviceConnector<>(keyboard, deviceRegistry, null, preferred, autoAdapt, false, true));
        }

        POSKeyboardManager posKeyboardManager = new POSKeyboardManager(
                new POSKeyboardDevice(
                        dynamicKeyboard,
                        new POSKeyboardDeviceListener(new EventSynchronizer(new Phaser(1)))),
                new ReentrantLock());

        DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().setPosKeyboardManager(posKeyboardManager);
        return posKeyboardManager;
    }

    @Bean
    SimulatedJposPOSKeyboard getSimulatedPOSKeyboard() {
        return simulatedPOSKeyboard;
    }
}
