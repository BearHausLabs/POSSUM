package com.target.devicemanager.components.keylock;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.keylock.entities.KeyLockStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jpos.JposConst;
import jpos.JposException;
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
 * REST Controller for KeyLock device operations.
 * Provides endpoints for reading positions, health checks, and device management.
 */
@RestController
@RequestMapping(value = "/v1")
@Tag(name = "KeyLock")
public class KeyLockController {
    
    private final KeyLockManager keyLockManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyLockController.class);
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    @Autowired
    public KeyLockController(KeyLockManager keyLockManager) {
        if (keyLockManager == null) {
            throw new IllegalArgumentException("keyLockManager cannot be null");
        }
        this.keyLockManager = keyLockManager;
    }
    
    /**
     * Get the current key lock position.
     * 
     * @return KeyLockStatus with current position
     */
    @Operation(description = "Get current key lock position")
    @GetMapping(path = "/keylock/position")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK - Position returned"),
            @ApiResponse(responseCode = "500", description = "UNEXPECTED_ERROR",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public ResponseEntity<KeyLockStatus> getPosition() {
        String url = "/v1/keylock/position";
        LOGGER.info("request: " + url);
        
        try {
            KeyLockStatus status = keyLockManager.getKeyPosition();
            LOGGER.info("response: " + url + " - 200 OK - Position: {}", status.getPositionName());
            return ResponseEntity.ok(status);
            
        } catch (JposException e) {
            LOGGER.error("response: " + url + " - Failed to get position: {} ({})", 
                       e.getErrorCode(), e.getErrorCodeExtended(), e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Stream position change events via Server-Sent Events (SSE).
     * 
     * @return SseEmitter for continuous position change streaming
     */
    @Operation(description = "Stream position change events continuously via SSE")
    @GetMapping(path = "/keylock/stream")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK - Streaming position changes")
    })
    public SseEmitter streamPositionChanges() {
        String url = "/v1/keylock/stream";
        LOGGER.info("request: " + url);
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        executor.execute(() -> {
            try {
                while (true) {
                    // Wait for any position change (10 second timeout)
                    try {
                        keyLockManager.waitForAnyChange(10000);
                        KeyLockStatus status = keyLockManager.getKeyPosition();
                        emitter.send(SseEmitter.event()
                                .name("position-change")
                                .data(status));
                        LOGGER.debug("Streamed position change event: {}", status.getPositionName());
                    } catch (JposException e) {
                        // Timeout is expected, continue waiting
                        if (e.getErrorCode() != JposConst.JPOS_E_TIMEOUT) {
                            throw e;
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("SSE stream error", e);
                emitter.completeWithError(e);
            } catch (JposException e) {
                LOGGER.error("KeyLock error", e);
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
    
    /**
     * Reports the health of the KeyLock.
     * 
     * @return List of device health responses
     */
    @Operation(description = "Reports the health of KeyLock")
    @GetMapping(path = "/keylock/health")
    public ResponseEntity<List<DeviceHealthResponse>> getHealth() {
        String url = "/v1/keylock/health";
        LOGGER.info("request: " + url);
        
        List<DeviceHealthResponse> responseList = keyLockManager.getHealth();
        
        for (DeviceHealthResponse deviceResponse : responseList) {
            LOGGER.info("response: " + url + " - " + deviceResponse.toString());
        }
        return ResponseEntity.ok(responseList);
    }
    
    /**
     * Reports KeyLock cached status.
     * 
     * @return List of device health responses
     */
    @Operation(description = "Reports KeyLock cached status")
    @GetMapping(path = "/keylock/healthstatus")
    public ResponseEntity<List<DeviceHealthResponse>> getStatus() {
        String url = "/v1/keylock/healthstatus";
        LOGGER.info("request: " + url);
        
        List<DeviceHealthResponse> responseList = keyLockManager.getStatus();
        
        for (DeviceHealthResponse deviceResponse : responseList) {
            LOGGER.info("response: " + url + " - " + deviceResponse.toString());
        }
        return ResponseEntity.ok(responseList);
    }
    
    /**
     * Reconnects the KeyLock.
     * 
     * @throws DeviceException if reconnection fails
     */
    @Operation(description = "Reconnects KeyLock")
    @PostMapping(path = "/keylock/reconnect")
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
        String url = "/v1/keylock/reconnect";
        LOGGER.info("request: " + url);
        
        try {
            keyLockManager.reconnectKeyLock();
            LOGGER.info("response: " + url + " - 200 OK");
        } catch (DeviceException deviceException) {
            LOGGER.info("response: " + url + " - " + deviceException.getDeviceError().getStatusCode().toString() + ", " + deviceException.getDeviceError());
            throw deviceException;
        }
    }
}

