package com.target.devicemanager.components.poskeyboard;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.poskeyboard.entities.POSKeyData;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST Controller for POSKeyboard device operations.
 * Provides endpoints for reading keys, health checks, and device management.
 */
@RestController
@RequestMapping(value = "/v1")
@Tag(name = "POSKeyboard")
public class POSKeyboardController {
    
    private final POSKeyboardManager posKeyboardManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(POSKeyboardController.class);
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    @Autowired
    public POSKeyboardController(POSKeyboardManager posKeyboardManager) {
        if (posKeyboardManager == null) {
            throw new IllegalArgumentException("posKeyboardManager cannot be null");
        }
        this.posKeyboardManager = posKeyboardManager;
    }
    
    /**
     * Read a single key press with optional timeout.
     * 
     * @param timeout Timeout in milliseconds (default 10000)
     * @return POSKeyData if key pressed, 204 if timeout
     */
    @Operation(description = "Read a single key press from POSKeyboard")
    @GetMapping(path = "/poskeyboard/key")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK - Key data returned"),
            @ApiResponse(responseCode = "204", description = "No key pressed within timeout"),
            @ApiResponse(responseCode = "500", description = "UNEXPECTED_ERROR",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public ResponseEntity<POSKeyData> getKey(
            @Parameter(description = "Timeout in milliseconds") 
            @RequestParam(defaultValue = "10000") long timeout) {
        String url = "/v1/poskeyboard/key";
        LOGGER.info("request: " + url + "?timeout=" + timeout);
        
        try {
            POSKeyData keyData = posKeyboardManager.readKey(timeout);
            
            if (keyData != null) {
                LOGGER.info("response: " + url + " - 200 OK - Key: {}", keyData.getKeyName());
                return ResponseEntity.ok(keyData);
            }
            
            LOGGER.info("response: " + url + " - 204 No Content");
            return ResponseEntity.noContent().build();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("response: " + url + " - Interrupted", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Stream key events via Server-Sent Events (SSE).
     * 
     * @return SseEmitter for continuous key event streaming
     */
    @Operation(description = "Stream key events continuously via SSE")
    @GetMapping(path = "/poskeyboard/stream")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK - Streaming key events")
    })
    public SseEmitter streamKeys() {
        String url = "/v1/poskeyboard/stream";
        LOGGER.info("request: " + url);
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        executor.execute(() -> {
            try {
                while (true) {
                    POSKeyData keyData = posKeyboardManager.readKey(0); // Wait forever
                    if (keyData != null) {
                        emitter.send(SseEmitter.event()
                                .name("keypress")
                                .data(keyData));
                        LOGGER.debug("Streamed key event: {}", keyData.getKeyName());
                    }
                }
            } catch (IOException e) {
                LOGGER.error("SSE stream error", e);
                emitter.completeWithError(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.info("Key stream interrupted");
                emitter.complete();
            }
        });
        
        return emitter;
    }
    
    /**
     * Clear any pending key data from the queue.
     * 
     * @return Empty response
     */
    @Operation(description = "Clear pending key data")
    @DeleteMapping(path = "/poskeyboard/key")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Key data cleared")
    })
    public ResponseEntity<Void> clearKeys() {
        String url = "/v1/poskeyboard/key";
        LOGGER.info("request: DELETE - " + url);
        
        posKeyboardManager.clearKeyData();
        
        LOGGER.info("response: DELETE " + url + " - 200 OK");
        return ResponseEntity.ok().build();
    }
    
    /**
     * Reports the health of the POSKeyboard.
     * 
     * @return List of device health responses
     */
    @Operation(description = "Reports the health of POSKeyboard")
    @GetMapping(path = "/poskeyboard/health")
    public ResponseEntity<List<DeviceHealthResponse>> getHealth() {
        String url = "/v1/poskeyboard/health";
        LOGGER.info("request: " + url);
        
        List<DeviceHealthResponse> responseList = posKeyboardManager.getHealth();
        
        for (DeviceHealthResponse deviceResponse : responseList) {
            LOGGER.info("response: " + url + " - " + deviceResponse.toString());
        }
        return ResponseEntity.ok(responseList);
    }
    
    /**
     * Reports POSKeyboard cached status.
     * 
     * @return List of device health responses
     */
    @Operation(description = "Reports POSKeyboard cached status")
    @GetMapping(path = "/poskeyboard/healthstatus")
    public ResponseEntity<List<DeviceHealthResponse>> getStatus() {
        String url = "/v1/poskeyboard/healthstatus";
        LOGGER.info("request: " + url);
        
        List<DeviceHealthResponse> responseList = posKeyboardManager.getStatus();
        
        for (DeviceHealthResponse deviceResponse : responseList) {
            LOGGER.info("response: " + url + " - " + deviceResponse.toString());
        }
        return ResponseEntity.ok(responseList);
    }
    
    /**
     * Reconnects the POSKeyboard.
     * 
     * @throws DeviceException if reconnection fails
     */
    @Operation(description = "Reconnects POSKeyboard")
    @PostMapping(path = "/poskeyboard/reconnect")
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
        String url = "/v1/poskeyboard/reconnect";
        LOGGER.info("request: " + url);
        
        try {
            posKeyboardManager.reconnectKeyboard();
            LOGGER.info("response: " + url + " - 200 OK");
        } catch (DeviceException deviceException) {
            LOGGER.info("response: " + url + " - " + deviceException.getDeviceError().getStatusCode().toString() + ", " + deviceException.getDeviceError());
            throw deviceException;
        }
    }
}

