package com.target.devicemanager.components.scanner;

import com.target.devicemanager.common.*;
import com.target.devicemanager.components.scanner.entities.ScannerType;
import com.target.devicemanager.components.scanner.simulator.SimulatedJposScanner;
import com.target.devicemanager.configuration.ApplicationConfig;
import com.target.devicemanager.configuration.WorkstationConfig;
import jpos.Scanner;
import jpos.config.JposEntryRegistry;
import jpos.loader.JposServiceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Phaser;
import java.util.concurrent.locks.ReentrantLock;

@Configuration
@ConditionalOnExpression(
        "'${possum.device.flatbedScanner.enabled:false}' == 'true' or '${possum.device.handScanner.enabled:false}' == 'true'")
class ScannerConfig {
    private final SimulatedJposScanner simulatedScanner;
    private final ApplicationConfig applicationConfig;
    private final WorkstationConfig workstationConfig;
    private final Environment environment;

    @Autowired
    ScannerConfig(ApplicationConfig applicationConfig, WorkstationConfig workstationConfig,
                  Environment environment) {
        this.applicationConfig = applicationConfig;
        this.workstationConfig = workstationConfig;
        this.environment = environment;
        this.simulatedScanner = new SimulatedJposScanner();
    }

    List<ScannerDevice> getScanners() {
        List<ScannerDevice> scanners = new ArrayList<>();
        JposEntryRegistry deviceRegistry = JposServiceLoader.getManager().getEntryRegistry();
        boolean autoAdapt = workstationConfig.isAutoAdapt();

        boolean flatbedEnabled = "true".equals(environment.getProperty("possum.device.flatbedScanner.enabled"));
        boolean handEnabled = "true".equals(environment.getProperty("possum.device.handScanner.enabled"));

        String preferredFlatbed = environment.getProperty("possum.device.flatbedScanner.logicalName");
        if (preferredFlatbed == null) {
            preferredFlatbed = workstationConfig.getDeviceLogicalName("flatbedScanner");
        }
        String preferredHand = environment.getProperty("possum.device.handScanner.logicalName");
        if (preferredHand == null) {
            preferredHand = workstationConfig.getDeviceLogicalName("handScanner");
        }

        if (applicationConfig.IsSimulationMode()) {
            scanners.add(new ScannerDevice(
                    new ScannerDeviceListener(new EventSynchronizer(new Phaser(1))),
                    new SimulatedDynamicDevice<>(simulatedScanner, new DevicePower(), new DeviceConnector<>(simulatedScanner, deviceRegistry)),
                    ScannerType.FLATBED, applicationConfig));
        } else {
            if (flatbedEnabled) {
                Scanner flatbedScanner = new Scanner();
                scanners.add(new ScannerDevice(
                        new ScannerDeviceListener(new EventSynchronizer(new Phaser(1))),
                        new DynamicDevice<>(flatbedScanner, new DevicePower(), new DeviceConnector<>(flatbedScanner, deviceRegistry, new SimpleEntry<>("deviceType", "Flatbed"), preferredFlatbed, autoAdapt)),
                        ScannerType.FLATBED, applicationConfig));
            }

            if (handEnabled) {
                Scanner handScanner = new Scanner();
                scanners.add(new ScannerDevice(
                        new ScannerDeviceListener(new EventSynchronizer(new Phaser(1))),
                        new DynamicDevice<>(handScanner, new DevicePower(), new DeviceConnector<>(handScanner, deviceRegistry, new SimpleEntry<>("deviceType", "HandScanner"), preferredHand, autoAdapt)),
                        ScannerType.HANDHELD, applicationConfig));
            }
        }

        return scanners;
    }

    @Bean
    public ScannerManager getScannerManager() {
        ScannerManager scannerManager = new ScannerManager(getScanners(), new ReentrantLock());

        DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().setScannerManager(scannerManager);
        return scannerManager;
    }

    @Bean
    SimulatedJposScanner getSimulatedScanner() {
        return simulatedScanner;
    }
}
