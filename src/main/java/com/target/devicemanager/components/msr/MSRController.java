package com.target.devicemanager.components.msr;

import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.msr.entities.CardData;
import com.target.devicemanager.components.msr.entities.MSRException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/v1")
@Tag(name = "MSR")
public class MSRController {

    private final MSRManager msrManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(MSRController.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("MSR", "MSRController", LOGGER);

    @Autowired
    public MSRController(MSRManager msrManager) {
        if (msrManager == null) {
            throw new IllegalArgumentException("msrManager cannot be null");
        }
        this.msrManager = msrManager;
    }

    @Operation(description = "Retrieve card data from connected MSR")
    @GetMapping(path = "/msr/read")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "204", description = "Card read request was cancelled"),
            @ApiResponse(responseCode = "500", description = "UNEXPECTED_ERROR",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public CardData getCardData() throws MSRException {
        String url = "/v1/msr/read";
        log.success("API Request Received", 1);
        try {
            CardData data = msrManager.getData();
            log.successAPI("API Request Completed Successfully", 1, url, data == null ? null : data.toString(), 200);
            return data;
        } catch (MSRException msrException) {
            log.failureAPI("API Request Failed with MSRException", 13, url, msrException.getDeviceError() == null ? null : msrException.getDeviceError().toString(), msrException.getDeviceError() == null ? 0 : msrException.getDeviceError().getStatusCode().value(), msrException);
            throw msrException;
        }
    }

    @Operation(description = "Cancel previously requested card read")
    @DeleteMapping(path = "/msr/read")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card read request canceled. MSR has been disabled"),
            @ApiResponse(responseCode = "412", description = "ALREADY_DISABLED",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public void cancelReadRequest() throws MSRException {
        String url = "/v1/msr/read";
        log.success("API Request Received", 1);
        try {
            msrManager.cancelReadRequest();
            log.successAPI("API Request Completed Successfully", 1, url, "OK", 200);
        } catch (MSRException msrException) {
            log.failureAPI("API Request Failed with MSRException", 13, url, msrException.getDeviceError() == null ? null : msrException.getDeviceError().toString(), msrException.getDeviceError() == null ? 0 : msrException.getDeviceError().getStatusCode().value(), msrException);
            throw msrException;
        }
    }

    @Operation(description = "Reports the health of the MSR")
    @GetMapping(path = "/msr/health")
    public ResponseEntity<DeviceHealthResponse> getHealth() {
        String url = "/v1/msr/health";
        log.success("API Request Received", 1);
        DeviceHealthResponse response = msrManager.getHealth();
        log.successAPI("API Request Completed Successfully", 1, url, response == null ? null : response.toString(), 200);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "Reports MSR status")
    @GetMapping(path = "/msr/healthstatus")
    public ResponseEntity<DeviceHealthResponse> getStatus() {
        String url = "/v1/msr/healthstatus";
        log.success("API Request Received", 1);
        DeviceHealthResponse response = msrManager.getStatus();
        log.successAPI("API Request Completed Successfully", 1, url, response == null ? null : response.toString(), 200);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "Reconnects MSR")
    @PostMapping(path = "/msr/reconnect")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "500", description = "UNEXPECTED_ERROR",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    void reconnect() throws DeviceException {
        String url = "/v1/msr/reconnect";
        log.success("API Request Received", 1);
        try {
            msrManager.reconnectDevice();
            log.successAPI("API Request Completed Successfully", 1, url, "OK", 200);
        } catch (DeviceException deviceException) {
            log.failureAPI("API Request Failed with DeviceException", 13, url, deviceException.getDeviceError() == null ? null : deviceException.getDeviceError().toString(), deviceException.getDeviceError() == null ? 0 : deviceException.getDeviceError().getStatusCode().value(), deviceException);
            throw deviceException;
        }
    }
}
