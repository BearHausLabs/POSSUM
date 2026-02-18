package com.target.devicemanager.components.cashdrawer;

import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.cashdrawer.entities.CashDrawerError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/cashdrawer")
@Tag(name = "Cash Drawer")
@Profile({"local", "dev", "prod"})
@ConditionalOnExpression(
        "'${possum.device.cashDrawer1.enabled:false}' == 'true' or " +
        "'${possum.device.cashDrawer2.enabled:false}' == 'true' or " +
        "'${possum.device.cashDrawer3.enabled:false}' == 'true' or " +
        "'${possum.device.cashDrawer4.enabled:false}' == 'true'")
public class CashDrawerController {

    private final CashDrawerManager cashDrawerManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(CashDrawerController.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("CashDrawer", "CashDrawerController", LOGGER);

    @Autowired
    public CashDrawerController(CashDrawerManager cashDrawerManager) {
        if (cashDrawerManager == null) {
            throw new IllegalArgumentException("cashDrawerManager cannot be null");
        }
        this.cashDrawerManager = cashDrawerManager;
    }

    @Operation(description = "Opens the cash drawer and waits until the cash drawer is closed before returning. drawerId must be 1-4.")
    @PostMapping("/{drawerId}/open")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "412", description = "ALREADY_OPEN, OPEN_FAILED",
                    content = @Content(schema = @Schema(implementation = CashDrawerError.class))),
            @ApiResponse(responseCode = "500", description = "UNEXPECTED_ERROR",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public void openCashDrawer(@Parameter(description = "Drawer number (1-4)") @PathVariable int drawerId) throws DeviceException {
        String url = "/v1/cashdrawer/" + drawerId + "/open";
        log.successAPI("request", 1, url, null, 0);
        try {
            cashDrawerManager.openCashDrawer(drawerId);
            log.successAPI("response", 1, url, null, 200);
        } catch (DeviceException deviceException) {
            int statusCode = deviceException.getDeviceError().getStatusCode().value();
            log.failureAPI("response", 13, url, deviceException.getDeviceError().toString(), statusCode, deviceException);
            throw deviceException;
        }
    }

    @Operation(description = "Reconnects to the cash drawer. drawerId must be 1-4.")
    @PostMapping("/{drawerId}/reconnect")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public void reconnect(@Parameter(description = "Drawer number (1-4)") @PathVariable int drawerId) throws DeviceException {
        String url = "/v1/cashdrawer/" + drawerId + "/reconnect";
        log.successAPI("request", 1, url, null, 0);
        try {
            cashDrawerManager.reconnectDevice(drawerId);
            log.successAPI("response", 1, url, null, 200);
        } catch (DeviceException deviceException) {
            int statusCode = deviceException.getDeviceError().getStatusCode().value();
            log.failureAPI("response", 13, url, deviceException.getDeviceError().toString(), statusCode, deviceException);
            throw deviceException;
        }
    }

    @Operation(description = "Reports health for all cash drawers")
    @GetMapping("/health")
    public List<DeviceHealthResponse> getAllHealth() {
        String url = "/v1/cashdrawer/health";
        log.successAPI("request", 1, url, null, 0);
        List<DeviceHealthResponse> response = cashDrawerManager.getAllHealth();
        log.successAPI("response", 1, url, response.toString(), 200);
        return response;
    }

    @Operation(description = "Reports cash drawer health for the given drawer. drawerId must be 1-4.")
    @GetMapping("/{drawerId}/health")
    public DeviceHealthResponse getHealth(@Parameter(description = "Drawer number (1-4)") @PathVariable int drawerId) throws DeviceException {
        String url = "/v1/cashdrawer/" + drawerId + "/health";
        log.successAPI("request", 1, url, null, 0);
        DeviceHealthResponse response = cashDrawerManager.getHealth(drawerId);
        log.successAPI("response", 1, url, response.toString(), 200);
        return response;
    }

    @Operation(description = "Reports cash drawer status for the given drawer. drawerId must be 1-4.")
    @GetMapping("/{drawerId}/healthstatus")
    public DeviceHealthResponse getStatus(@Parameter(description = "Drawer number (1-4)") @PathVariable int drawerId) throws DeviceException {
        String url = "/v1/cashdrawer/" + drawerId + "/healthstatus";
        log.successAPI("request", 1, url, null, 0);
        DeviceHealthResponse response = cashDrawerManager.getStatus(drawerId);
        log.successAPI("response", 1, url, response.toString(), 200);
        return response;
    }
}
