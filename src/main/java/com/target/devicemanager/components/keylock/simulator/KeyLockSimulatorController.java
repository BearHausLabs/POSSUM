package com.target.devicemanager.components.keylock.simulator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jpos.KeylockConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for KeyLock simulator.
 * Allows simulating key lock position changes for testing without physical hardware.
 */
@RestController
@RequestMapping(value = "/sim")
@Tag(name = "KeyLock Simulator")
public class KeyLockSimulatorController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyLockSimulatorController.class);
    private final SimulatedJposKeylock simulatedKeylock;
    
    @Autowired
    public KeyLockSimulatorController(SimulatedJposKeylock simulatedKeylock) {
        this.simulatedKeylock = simulatedKeylock;
    }
    
    /**
     * Simulates a key lock position change.
     * 
     * @param position Position to change to (0=ANY, 1=LOCKED, 2=NORMAL, 3=SUPERVISOR)
     * @return Response entity
     */
    @Operation(description = "Simulate key lock position change for testing")
    @PostMapping(path = "/keylock/setposition")
    public ResponseEntity<String> setPosition(
            @Parameter(description = "Position: 0=ANY, 1=LOCKED, 2=NORMAL, 3=SUPERVISOR") 
            @RequestParam int position) {
        
        LOGGER.info("Simulator: Setting key lock position to {}", position);
        
        try {
            String positionName;
            switch (position) {
                case KeylockConst.LOCK_KP_LOCK:
                    positionName = "LOCKED";
                    break;
                case KeylockConst.LOCK_KP_NORM:
                    positionName = "NORMAL";
                    break;
                case KeylockConst.LOCK_KP_SUPR:
                    positionName = "SUPERVISOR";
                    break;
                case KeylockConst.LOCK_KP_ANY:
                    return ResponseEntity.badRequest().body("Cannot set position to ANY (0)");
                default:
                    return ResponseEntity.badRequest().body("Invalid position: " + position);
            }
            
            simulatedKeylock.simulatePositionChange(position);
            
            return ResponseEntity.ok("Position changed to: " + positionName);
        } catch (Exception e) {
            LOGGER.error("Error simulating position change", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Simulates changing to LOCKED position.
     * 
     * @return Response entity
     */
    @Operation(description = "Simulate changing to LOCKED position")
    @PostMapping(path = "/keylock/lock")
    public ResponseEntity<String> lock() {
        return setPosition(KeylockConst.LOCK_KP_LOCK);
    }
    
    /**
     * Simulates changing to NORMAL position.
     * 
     * @return Response entity
     */
    @Operation(description = "Simulate changing to NORMAL position")
    @PostMapping(path = "/keylock/normal")
    public ResponseEntity<String> normal() {
        return setPosition(KeylockConst.LOCK_KP_NORM);
    }
    
    /**
     * Simulates changing to SUPERVISOR position.
     * 
     * @return Response entity
     */
    @Operation(description = "Simulate changing to SUPERVISOR position")
    @PostMapping(path = "/keylock/supervisor")
    public ResponseEntity<String> supervisor() {
        return setPosition(KeylockConst.LOCK_KP_SUPR);
    }
}

