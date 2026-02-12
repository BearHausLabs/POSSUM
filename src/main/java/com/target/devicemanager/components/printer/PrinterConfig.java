package com.target.devicemanager.components.printer;

import com.target.devicemanager.common.*;
import com.target.devicemanager.components.printer.simulator.SimulatedJposPrinter;
import com.target.devicemanager.configuration.ApplicationConfig;
import com.target.devicemanager.configuration.WorkstationConfig;
import jpos.POSPrinter;
import jpos.config.JposEntryRegistry;
import jpos.loader.JposServiceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Phaser;
import java.util.concurrent.locks.ReentrantLock;

@Configuration
class PrinterConfig {
    private final SimulatedJposPrinter simulatedPrinter;
    private final ApplicationConfig applicationConfig;
    private final WorkstationConfig workstationConfig;

    @Autowired
    PrinterConfig(ApplicationConfig applicationConfig, WorkstationConfig workstationConfig) {

        this.simulatedPrinter = new SimulatedJposPrinter();
        this.applicationConfig = applicationConfig;
        this.workstationConfig = workstationConfig;
    }

    @Bean
    public PrinterManager getReceiptPrinterManager() {
        DynamicDevice<? extends POSPrinter> dynamicPrinter;
        JposEntryRegistry deviceRegistry = JposServiceLoader.getManager().getEntryRegistry();
        String preferred = workstationConfig.getDeviceLogicalName("printer");
        boolean autoAdapt = workstationConfig.isAutoAdapt();

        if (applicationConfig.IsSimulationMode()) {
            dynamicPrinter = new SimulatedDynamicDevice<>(simulatedPrinter, new DevicePower(), new DeviceConnector<>(simulatedPrinter, deviceRegistry));

        } else {
            POSPrinter posPrinter = new POSPrinter();
            dynamicPrinter = new DynamicDevice<>(posPrinter, new DevicePower(), new DeviceConnector<>(posPrinter, deviceRegistry, null, preferred, autoAdapt));
        }

        PrinterManager printerManager = new PrinterManager(
                new PrinterDevice(dynamicPrinter, new PrinterDeviceListener(new EventSynchronizer(new Phaser(1)))),
                new ReentrantLock());

        DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().setPrinterManager(printerManager);
        return printerManager;
    }

    @Bean
    SimulatedJposPrinter getMyPrinter() {
        return simulatedPrinter;
    }
}
