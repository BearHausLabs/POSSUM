package com.target.devicemanager.components.msr.simulator;

import com.target.devicemanager.common.SimulatorState;
import com.target.devicemanager.components.msr.entities.CardData;
import com.target.devicemanager.configuration.ApplicationConfig;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/v1/simulate")
@Tag(name = "MSR")
@ConditionalOnProperty(name = "possum.device.msr.enabled", havingValue = "true")
public class MSRSimulatorController {
    private final SimulatedJposMSR simulatedJposMSR;
    private final ApplicationConfig applicationConfig;

    @Autowired
    public MSRSimulatorController(ApplicationConfig applicationConfig, SimulatedJposMSR simulatedJposMSR) {
        if (applicationConfig == null) {
            throw new IllegalArgumentException("applicationConfig cannot be null");
        }

        if (simulatedJposMSR == null) {
            throw new IllegalArgumentException("simulatedJposMSR cannot be null");
        }

        this.simulatedJposMSR = simulatedJposMSR;
        this.applicationConfig = applicationConfig;
    }

    @Operation(description = "Set card data to complete the currently pending MSR read request")
    @PostMapping(path = "msr")
    public void setCardData(@RequestBody CardData cardData) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }
        simulatedJposMSR.setCardData(cardData);
    }

    @Operation(description = "Set current state of the MSR")
    @PostMapping(path = "msrState")
    public void setDeviceState(@RequestParam SimulatorState simulatorState) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }
        simulatedJposMSR.setState(simulatorState);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<String> handleException(UnsupportedOperationException exception) {
        return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
    }
}
