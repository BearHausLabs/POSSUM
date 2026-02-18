package com.target.devicemanager.components.cashdrawer.simulator;

import com.target.devicemanager.common.SimulatorState;
import com.target.devicemanager.configuration.ApplicationConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/v1/simulate")
@Tag(name = "Cash Drawer")
@Profile("local")
@ConditionalOnExpression(
        "'${possum.device.cashDrawer1.enabled:false}' == 'true' or " +
        "'${possum.device.cashDrawer2.enabled:false}' == 'true' or " +
        "'${possum.device.cashDrawer3.enabled:false}' == 'true' or " +
        "'${possum.device.cashDrawer4.enabled:false}' == 'true'")
public class CashDrawerSimulatorController {
    private final SimulatedJposCashDrawer simulatedJposCashDrawer;
    private final ApplicationConfig applicationConfig;

    public CashDrawerSimulatorController(ApplicationConfig applicationConfig, SimulatedJposCashDrawer simulatedJposCashDrawer) {
        if (applicationConfig == null) {
            throw new IllegalArgumentException("applicationConfig cannot be null");
        }

        if (simulatedJposCashDrawer == null) {
            throw new IllegalArgumentException("simulatedJposCashDrawer cannot be null");
        }

        this.simulatedJposCashDrawer = simulatedJposCashDrawer;
        this.applicationConfig = applicationConfig;
    }

    @Operation(description = "Set current status of the cash drawer. drawerId must be 1-4.")
    @PostMapping(path = "{drawerId}/cashdrawerStatus")
    public void setDeviceStatus(@Parameter(description = "Drawer number (1-4)") @PathVariable int drawerId,
                               @RequestParam CashDrawerStatus cashDrawerStatus) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }

        simulatedJposCashDrawer.setStatus(cashDrawerStatus);
    }

    @Operation(description = "Set current state of the cash drawer. drawerId must be 1-4.")
    @PostMapping(path = "{drawerId}/cashdrawerState")
    public void setDeviceState(@Parameter(description = "Drawer number (1-4)") @PathVariable int drawerId,
                              @RequestParam SimulatorState simulatorState) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }

        simulatedJposCashDrawer.setState(simulatorState);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<String> handleException(UnsupportedOperationException exception) {
        return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
    }
}
