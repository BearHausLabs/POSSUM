package com.target.devicemanager.components.poskeyboard.simulator;

import com.target.devicemanager.common.SimulatorState;
import com.target.devicemanager.configuration.ApplicationConfig;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/v1/simulate")
@Tag(name = "POSKeyboard")
@Profile("local")
@ConditionalOnProperty(name = "possum.device.posKeyboard.enabled", havingValue = "true")
public class POSKeyboardSimulatorController {
    private final SimulatedJposPOSKeyboard simulatedJposPOSKeyboard;
    private final ApplicationConfig applicationConfig;

    public POSKeyboardSimulatorController(ApplicationConfig applicationConfig, SimulatedJposPOSKeyboard simulatedJposPOSKeyboard) {
        if (applicationConfig == null) {
            throw new IllegalArgumentException("applicationConfig cannot be null");
        }

        if (simulatedJposPOSKeyboard == null) {
            throw new IllegalArgumentException("simulatedJposPOSKeyboard cannot be null");
        }

        this.simulatedJposPOSKeyboard = simulatedJposPOSKeyboard;
        this.applicationConfig = applicationConfig;
    }

    @Operation(description = "Simulate a key press on the POS keyboard")
    @PostMapping(path = "poskeyboardKey")
    public void simulateKeyPress(@RequestParam int keyCode) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }

        simulatedJposPOSKeyboard.simulateKeyPress(keyCode);
    }

    @Operation(description = "Set current state of the POS keyboard")
    @PostMapping(path = "poskeyboardState")
    public void setDeviceState(@RequestParam SimulatorState simulatorState) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }

        simulatedJposPOSKeyboard.setState(simulatorState);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<String> handleException(UnsupportedOperationException exception) {
        return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
    }
}
