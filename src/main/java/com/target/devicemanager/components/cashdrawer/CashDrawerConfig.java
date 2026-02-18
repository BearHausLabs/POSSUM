package com.target.devicemanager.components.cashdrawer;

import com.target.devicemanager.common.*;
import com.target.devicemanager.components.cashdrawer.simulator.SimulatedJposCashDrawer;
import com.target.devicemanager.configuration.ApplicationConfig;
import com.target.devicemanager.configuration.WorkstationConfig;
import jpos.CashDrawer;
import jpos.config.JposEntryRegistry;
import jpos.loader.JposServiceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.concurrent.locks.ReentrantLock;

@Configuration
@Profile({"local", "dev", "prod"})
@ConditionalOnExpression(
        "'${possum.device.cashDrawer1.enabled:false}' == 'true' or " +
        "'${possum.device.cashDrawer2.enabled:false}' == 'true' or " +
        "'${possum.device.cashDrawer3.enabled:false}' == 'true' or " +
        "'${possum.device.cashDrawer4.enabled:false}' == 'true'")
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
        Map<Integer, CashDrawerDevice> devices = new LinkedHashMap<>();
        JposEntryRegistry deviceRegistry = JposServiceLoader.getManager().getEntryRegistry();
        boolean autoAdapt = workstationConfig.isAutoAdapt();

        if (applicationConfig.IsSimulationMode()) {
            DynamicDevice<? extends CashDrawer> dynamicCashDrawer = new SimulatedDynamicDevice<>(
                    simulatedCashDrawer, new DevicePower(), new DeviceConnector<>(simulatedCashDrawer, deviceRegistry));
            devices.put(1, new CashDrawerDevice(
                    dynamicCashDrawer,
                    new CashDrawerDeviceListener(new EventSynchronizer(new Phaser(1)))));
        } else {
            for (int i = 1; i <= 4; i++) {
                String key = "cashDrawer" + i;
                if (!"true".equals(environment.getProperty("possum.device." + key + ".enabled"))) {
                    continue;
                }
                String preferred = environment.getProperty("possum.device." + key + ".logicalName");
                if (preferred == null) {
                    preferred = workstationConfig.getDeviceLogicalName(key);
                }
                CashDrawer cashDrawer = new CashDrawer();
                DynamicDevice<? extends CashDrawer> dynamicCashDrawer = new DynamicDevice<>(
                        cashDrawer, new DevicePower(),
                        new DeviceConnector<>(cashDrawer, deviceRegistry, null, preferred, autoAdapt));
                devices.put(i, new CashDrawerDevice(
                        dynamicCashDrawer,
                        new CashDrawerDeviceListener(new EventSynchronizer(new Phaser(1)))));
            }
        }

        CashDrawerManager cashDrawerManager = new CashDrawerManager(devices, new ReentrantLock());
        DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().setCashDrawerManager(cashDrawerManager);
        return cashDrawerManager;
    }

    @Bean
    SimulatedJposCashDrawer getSimulatedCashDrawer() {
        return simulatedCashDrawer;
    }
}
