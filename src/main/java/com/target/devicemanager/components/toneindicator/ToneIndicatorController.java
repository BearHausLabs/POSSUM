package com.target.devicemanager.components.toneindicator;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.toneindicator.entities.ToneRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jpos.JposException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for ToneIndicator device operations.
 * Provides endpoints for playing tones, health checks, and device management.
 */
@RestController
@RequestMapping(value = "/v1")
@Tag(name = "ToneIndicator")
public class ToneIndicatorController {
    
    private final ToneIndicatorManager toneIndicatorManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(ToneIndicatorController.class);
    
    @Autowired
    public ToneIndicatorController(ToneIndicatorManager toneIndicatorManager) {
        if (toneIndicatorManager == null) {
            throw new IllegalArgumentException("toneIndicatorManager cannot be null");
        }
        this.toneIndicatorManager = toneIndicatorManager;
    }
    
    /**
     * Play a custom tone with specified parameters.
     * 
     * @param toneRequest The tone parameters
     * @return Response entity
     */
    @Operation(description = "Play a custom tone with specified parameters")
    @PostMapping(path = "/toneindicator/sound")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK - Tone played successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid parameters",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "500", description = "UNEXPECTED_ERROR",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public ResponseEntity<String> sound(@Valid @RequestBody ToneRequest toneRequest) {
        String url = "/v1/toneindicator/sound";
        LOGGER.info("request: " + url + " - " + toneRequest.toString());
        
        try {
            toneIndicatorManager.sound(toneRequest);
            LOGGER.info("response: " + url + " - 200 OK");
            return ResponseEntity.ok("Tone played successfully");
            
        } catch (JposException e) {
            LOGGER.error("response: " + url + " - Failed to play tone: {} ({})", 
                       e.getErrorCode(), e.getErrorCodeExtended(), e);
            return ResponseEntity.status(500).body("Failed to play tone: " + e.getMessage());
        }
    }
    
    /**
     * Play a default beep sound.
     * 
     * @return Response entity
     */
    @Operation(description = "Play a default beep sound")
    @PostMapping(path = "/toneindicator/beep")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK - Beep played successfully"),
            @ApiResponse(responseCode = "500", description = "UNEXPECTED_ERROR",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public ResponseEntity<String> beep() {
        String url = "/v1/toneindicator/beep";
        LOGGER.info("request: " + url);
        
        try {
            toneIndicatorManager.soundImmediate();
            LOGGER.info("response: " + url + " - 200 OK");
            return ResponseEntity.ok("Beep played successfully");
            
        } catch (JposException e) {
            LOGGER.error("response: " + url + " - Failed to play beep: {} ({})", 
                       e.getErrorCode(), e.getErrorCodeExtended(), e);
            return ResponseEntity.status(500).body("Failed to play beep: " + e.getMessage());
        }
    }
    
    /**
     * Reports the health of the ToneIndicator.
     * 
     * @return List of device health responses
     */
    @Operation(description = "Reports the health of ToneIndicator")
    @GetMapping(path = "/toneindicator/health")
    public ResponseEntity<List<DeviceHealthResponse>> getHealth() {
        String url = "/v1/toneindicator/health";
        LOGGER.info("request: " + url);
        
        List<DeviceHealthResponse> responseList = toneIndicatorManager.getHealth();
        
        for (DeviceHealthResponse deviceResponse : responseList) {
            LOGGER.info("response: " + url + " - " + deviceResponse.toString());
        }
        return ResponseEntity.ok(responseList);
    }
    
    /**
     * Reports ToneIndicator cached status.
     * 
     * @return List of device health responses
     */
    @Operation(description = "Reports ToneIndicator cached status")
    @GetMapping(path = "/toneindicator/healthstatus")
    public ResponseEntity<List<DeviceHealthResponse>> getStatus() {
        String url = "/v1/toneindicator/healthstatus";
        LOGGER.info("request: " + url);
        
        List<DeviceHealthResponse> responseList = toneIndicatorManager.getStatus();
        
        for (DeviceHealthResponse deviceResponse : responseList) {
            LOGGER.info("response: " + url + " - " + deviceResponse.toString());
        }
        return ResponseEntity.ok(responseList);
    }
    
    /**
     * Reconnects the ToneIndicator.
     * 
     * @throws DeviceException if reconnection fails
     */
    @Operation(description = "Reconnects ToneIndicator")
    @PostMapping(path = "/toneindicator/reconnect")
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
        String url = "/v1/toneindicator/reconnect";
        LOGGER.info("request: " + url);
        
        try {
            toneIndicatorManager.reconnectToneIndicator();
            LOGGER.info("response: " + url + " - 200 OK");
        } catch (DeviceException deviceException) {
            LOGGER.info("response: " + url + " - " + deviceException.getDeviceError().getStatusCode().toString() + ", " + deviceException.getDeviceError());
            throw deviceException;
        }
    }
}

