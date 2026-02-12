package com.target.devicemanager.components.toneindicator;

import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.toneindicator.entities.ToneIndicatorError;
import com.target.devicemanager.components.toneindicator.entities.ToneRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/toneindicator")
@Tag(name = "Tone Indicator")
@Profile({"local", "dev", "prod"})
public class ToneIndicatorController {

    private final ToneIndicatorManager toneIndicatorManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(ToneIndicatorController.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("ToneIndicator", "ToneIndicatorController", LOGGER);

    @Autowired
    public ToneIndicatorController(ToneIndicatorManager toneIndicatorManager) {
        if (toneIndicatorManager == null) {
            throw new IllegalArgumentException("toneIndicatorManager cannot be null");
        }
        this.toneIndicatorManager = toneIndicatorManager;
    }

    @Operation(description = "Plays a tone with the specified parameters and returns when the tone finishes.")
    @PostMapping("/sound")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "412", description = "SOUND_FAILED",
                    content = @Content(schema = @Schema(implementation = ToneIndicatorError.class))),
            @ApiResponse(responseCode = "500", description = "UNEXPECTED_ERROR",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public void playSound(@RequestBody ToneRequest toneRequest) throws DeviceException {
        String url = "/v1/toneindicator/sound";
        log.successAPI("request", 1, url, toneRequest.toString(), 0);
        try {
            toneIndicatorManager.playSound(toneRequest);
            log.successAPI("response", 1, url, null, 200);
        } catch (DeviceException deviceException) {
            int statusCode = deviceException.getDeviceError().getStatusCode().value();
            log.failureAPI("response", 13, url, deviceException.getDeviceError().toString(), statusCode, deviceException);
            throw deviceException;
        }
    }

    @Operation(description = "Reconnects to the tone indicator")
    @PostMapping("/reconnect")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public void reconnect() throws DeviceException {
        String url = "/v1/toneindicator/reconnect";
        log.successAPI("request", 1, url, null, 0);
        try {
            toneIndicatorManager.reconnectDevice();
            log.successAPI("response", 1, url, null, 200);
        } catch (DeviceException deviceException) {
            int statusCode = deviceException.getDeviceError().getStatusCode().value();
            log.failureAPI("response", 13, url, deviceException.getDeviceError().toString(), statusCode, deviceException);
            throw deviceException;
        }
    }

    @Operation(description = "Reports tone indicator health")
    @GetMapping("/health")
    public DeviceHealthResponse getHealth() {
        String url = "/v1/toneindicator/health";
        log.successAPI("request", 1, url, null, 0);
        DeviceHealthResponse response = toneIndicatorManager.getHealth();
        log.successAPI("response", 1, url, response.toString(), 200);
        return response;
    }

    @Operation(description = "Reports tone indicator status")
    @GetMapping("/healthstatus")
    public DeviceHealthResponse getStatus() {
        String url = "/v1/toneindicator/healthstatus";
        log.successAPI("request", 1, url, null, 0);
        DeviceHealthResponse response = toneIndicatorManager.getStatus();
        log.successAPI("response", 1, url, response.toString(), 200);
        return response;
    }
}
