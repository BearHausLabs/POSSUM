package com.target.devicemanager.components.poskeyboard.simulator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jpos.POSKeyboardConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for POSKeyboard simulator.
 * Allows injecting simulated key events for testing without physical hardware.
 */
@RestController
@RequestMapping(value = "/sim")
@Tag(name = "POSKeyboard Simulator")
public class POSKeyboardSimulatorController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(POSKeyboardSimulatorController.class);
    private final SimulatedJposPOSKeyboard simulatedPOSKeyboard;
    
    @Autowired
    public POSKeyboardSimulatorController(SimulatedJposPOSKeyboard simulatedPOSKeyboard) {
        this.simulatedPOSKeyboard = simulatedPOSKeyboard;
    }
    
    /**
     * Simulates a key press event.
     * 
     * @param keyCode ASCII key code to simulate (e.g., 65 for 'A')
     * @param eventType Event type: 1=KEY_DOWN, 2=KEY_UP, 0=BOTH (default)
     * @return Response entity
     */
    @Operation(description = "Inject a simulated key press for testing")
    @PostMapping(path = "/poskeyboard/sendkey")
    public ResponseEntity<String> sendKey(
            @Parameter(description = "ASCII key code (e.g., 65 for 'A')") 
            @RequestParam int keyCode,
            @Parameter(description = "Event type: 1=DOWN, 2=UP, 0=BOTH (default 0)") 
            @RequestParam(defaultValue = "0") int eventType) {
        
        LOGGER.info("Simulator: Sending key - keyCode={} (0x{}), eventType={}", 
                   keyCode, Integer.toHexString(keyCode), eventType);
        
        try {
            if (eventType == 0) {
                // Simulate both down and up
                simulatedPOSKeyboard.simulateKeyPress(keyCode);
            } else if (eventType == POSKeyboardConst.KBD_KET_KEYDOWN) {
                simulatedPOSKeyboard.simulateKeyPress(keyCode, POSKeyboardConst.KBD_KET_KEYDOWN);
            } else if (eventType == POSKeyboardConst.KBD_KET_KEYUP) {
                simulatedPOSKeyboard.simulateKeyPress(keyCode, POSKeyboardConst.KBD_KET_KEYUP);
            } else {
                return ResponseEntity.badRequest().body("Invalid event type: " + eventType);
            }
            
            return ResponseEntity.ok("Key simulated successfully");
        } catch (Exception e) {
            LOGGER.error("Error simulating key press", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Simulates typing a string of characters.
     * 
     * @param text Text to type
     * @return Response entity
     */
    @Operation(description = "Simulate typing a string of characters")
    @PostMapping(path = "/poskeyboard/type")
    public ResponseEntity<String> typeText(
            @Parameter(description = "Text string to type") 
            @RequestParam String text) {
        
        LOGGER.info("Simulator: Typing text - '{}'", text);
        
        try {
            for (char c : text.toCharArray()) {
                simulatedPOSKeyboard.simulateKeyPress((int) c);
                Thread.sleep(100); // Small delay between keys
            }
            
            return ResponseEntity.ok("Text typed successfully: " + text.length() + " characters");
        } catch (Exception e) {
            LOGGER.error("Error typing text", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}

