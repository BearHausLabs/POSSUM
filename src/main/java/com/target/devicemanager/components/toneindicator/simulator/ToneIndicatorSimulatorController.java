package com.target.devicemanager.components.toneindicator.simulator;

import com.target.devicemanager.common.SimulatorState;
import com.target.devicemanager.configuration.ApplicationConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/v1/simulate")
@Tag(name = "Tone Indicator")
@Profile("local")
public class ToneIndicatorSimulatorController {
    private final SimulatedJposToneIndicator simulatedJposToneIndicator;
    private final ApplicationConfig applicationConfig;

    public ToneIndicatorSimulatorController(ApplicationConfig applicationConfig, SimulatedJposToneIndicator simulatedJposToneIndicator) {
        if (applicationConfig == null) {
            throw new IllegalArgumentException("applicationConfig cannot be null");
        }

        if (simulatedJposToneIndicator == null) {
            throw new IllegalArgumentException("simulatedJposToneIndicator cannot be null");
        }

        this.simulatedJposToneIndicator = simulatedJposToneIndicator;
        this.applicationConfig = applicationConfig;
    }

    @Operation(description = "Set current state of the tone indicator")
    @PostMapping(path = "toneIndicatorState")
    public void setDeviceState(@RequestParam SimulatorState simulatorState) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }

        simulatedJposToneIndicator.setState(simulatorState);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<String> handleException(UnsupportedOperationException exception) {
        return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
    }
}
