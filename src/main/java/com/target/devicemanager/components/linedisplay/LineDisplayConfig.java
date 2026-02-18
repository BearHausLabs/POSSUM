package com.target.devicemanager.components.linedisplay;

import com.target.devicemanager.common.*;
import com.target.devicemanager.components.linedisplay.simulator.SimulatedJposLineDisplay;
import com.target.devicemanager.configuration.ApplicationConfig;
import com.target.devicemanager.configuration.WorkstationConfig;
import jpos.LineDisplay;
import jpos.config.JposEntryRegistry;
import jpos.loader.JposServiceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@Profile({"local","dev","prod"})
@ConditionalOnProperty(name = "possum.device.lineDisplay.enabled", havingValue = "true")
class LineDisplayConfig {

    private final SimulatedJposLineDisplay simulatedLineDisplay;
    private final ApplicationConfig applicationConfig;
    private final WorkstationConfig workstationConfig;
    private final Environment environment;

    @Autowired
    LineDisplayConfig(ApplicationConfig applicationConfig, WorkstationConfig workstationConfig, Environment environment) {
        this.applicationConfig = applicationConfig;
        this.workstationConfig = workstationConfig;
        this.environment = environment;
        this.simulatedLineDisplay = new SimulatedJposLineDisplay();
    }

    @Bean
    public LineDisplayManager getLineDisplayManager() {
        DynamicDevice<LineDisplay> dynamicLineDisplay;
        JposEntryRegistry deviceRegistry = JposServiceLoader.getManager().getEntryRegistry();
        String preferred = environment.getProperty("possum.device.lineDisplay.logicalName");
        if (preferred == null) {
            preferred = workstationConfig.getDeviceLogicalName("lineDisplay");
        }
        boolean autoAdapt = workstationConfig.isAutoAdapt();

        if (applicationConfig.IsSimulationMode()) {
            dynamicLineDisplay = new DynamicDevice<>(simulatedLineDisplay, new DevicePower(), new DeviceConnector<>(simulatedLineDisplay, deviceRegistry));
        } else {
            LineDisplay lineDisplay = new LineDisplay();
            dynamicLineDisplay = new DynamicDevice<>(lineDisplay, new DevicePower(), new DeviceConnector<>(lineDisplay, deviceRegistry, null, preferred, autoAdapt));
        }

        LineDisplayManager lineDisplayManager = new LineDisplayManager(
                new LineDisplayDevice(dynamicLineDisplay));

        DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().setLineDisplayManager(lineDisplayManager);
        return lineDisplayManager;
    }

    @Bean
    SimulatedJposLineDisplay getSimulatedLineDisplay() {
        return simulatedLineDisplay;
    }
}
