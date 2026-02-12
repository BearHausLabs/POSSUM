package com.target.devicemanager.components.poskeyboard;

import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v1/poskeyboard")
@Tag(name = "POSKeyboard")
@Profile({"local", "dev", "prod"})
public class POSKeyboardController {

    private final POSKeyboardManager posKeyboardManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(POSKeyboardController.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("POSKeyboard", "POSKeyboardController", LOGGER);

    @Autowired
    public POSKeyboardController(POSKeyboardManager posKeyboardManager) {
        if (posKeyboardManager == null) {
            throw new IllegalArgumentException("posKeyboardManager cannot be null");
        }
        this.posKeyboardManager = posKeyboardManager;
    }

    @Operation(description = "Subscribe to real-time keyboard events via Server-Sent Events (SSE). " +
            "Events are pushed as JSON objects containing keyCode, eventType (KEY_DOWN/KEY_UP), and timestamp.")
    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE stream opened")
    })
    public SseEmitter subscribeToKeyboardEvents() {
        String url = "/v1/poskeyboard/events";
        log.successAPI("request", 1, url, null, 0);
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        posKeyboardManager.addEventSubscriber(emitter);
        log.successAPI("response", 1, url, "SSE stream opened", 200);
        return emitter;
    }

    @Operation(description = "Reconnects to the POS keyboard device")
    @PostMapping("/reconnect")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public void reconnect() throws DeviceException {
        String url = "/v1/poskeyboard/reconnect";
        log.successAPI("request", 1, url, null, 0);
        try {
            posKeyboardManager.reconnectDevice();
            log.successAPI("response", 1, url, null, 200);
        } catch (DeviceException deviceException) {
            int statusCode = deviceException.getDeviceError().getStatusCode().value();
            log.failureAPI("response", 13, url, deviceException.getDeviceError().toString(), statusCode, deviceException);
            throw deviceException;
        }
    }

    @Operation(description = "Reports POS keyboard health")
    @GetMapping("/health")
    public DeviceHealthResponse getHealth() {
        String url = "/v1/poskeyboard/health";
        log.successAPI("request", 1, url, null, 0);
        DeviceHealthResponse response = posKeyboardManager.getHealth();
        log.successAPI("response", 1, url, response.toString(), 200);
        return response;
    }

    @Operation(description = "Reports POS keyboard status")
    @GetMapping("/healthstatus")
    public DeviceHealthResponse getStatus() {
        String url = "/v1/poskeyboard/healthstatus";
        log.successAPI("request", 1, url, null, 0);
        DeviceHealthResponse response = posKeyboardManager.getStatus();
        log.successAPI("response", 1, url, response.toString(), 200);
        return response;
    }
}
