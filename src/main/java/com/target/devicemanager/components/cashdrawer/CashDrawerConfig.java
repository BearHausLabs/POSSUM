package com.target.devicemanager.components.cashdrawer;

import com.target.devicemanager.common.*;
import com.target.devicemanager.components.cashdrawer.simulator.SimulatedJposCashDrawer;
import com.target.devicemanager.configuration.ApplicationConfig;
import com.target.devicemanager.configuration.WorkstationConfig;
import jpos.CashDrawer;
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
@ConditionalOnProperty(name = "possum.device.cashDrawer.enabled", havingValue = "true")
class CashDrawerConfig {
    private final SimulatedJposCashDrawer simulatedCashDrawer;
    private final ApplicationConfig applicationConfig;
    private final WorkstationConfig workstationConfig;
    private final Environment environment;

    @Autowired
    CashDrawerConfig(ApplicationConfig applicationConfig, WorkstationConfig workstationConfig, Environment environment) {
        this.applicationConfig = applicationConfig;
        this.workstationConfig = workstationConfig;
        this.environment = environment;
        this.simulatedCashDrawer = new SimulatedJposCashDrawer();
    }

    @Bean
    public CashDrawerManager getCashDrawerManager() {
        DynamicDevice<? extends CashDrawer> dynamicCashDrawer;
        JposEntryRegistry deviceRegistry = JposServiceLoader.getManager().getEntryRegistry();
        String preferred = environment.getProperty("possum.device.cashDrawer.logicalName");
        if (preferred == null) {
            preferred = workstationConfig.getDeviceLogicalName("cashDrawer");
        }
        boolean autoAdapt = workstationConfig.isAutoAdapt();
        if (applicationConfig.IsSimulationMode()) {
            dynamicCashDrawer = new SimulatedDynamicDevice<>(simulatedCashDrawer, new DevicePower(), new DeviceConnector<>(simulatedCashDrawer, deviceRegistry));

        } else {
            CashDrawer cashDrawer = new CashDrawer();
            dynamicCashDrawer = new DynamicDevice<>(cashDrawer, new DevicePower(), new DeviceConnector<>(cashDrawer, deviceRegistry, null, preferred, autoAdapt));
        }

        CashDrawerManager cashDrawerManager = new CashDrawerManager(
                new CashDrawerDevice(
                        dynamicCashDrawer,
                        new CashDrawerDeviceListener(new EventSynchronizer(new Phaser(1)))),
                new ReentrantLock());

        DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().setCashDrawerManager(cashDrawerManager);
        return cashDrawerManager;
    }

    @Bean
    SimulatedJposCashDrawer getSimulatedCashDrawer() {
        return simulatedCashDrawer;
    }

}
