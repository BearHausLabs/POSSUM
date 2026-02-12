package com.target.devicemanager.common;

import com.target.devicemanager.common.entities.DeviceDiscoveryResponse;
import com.target.devicemanager.common.entities.DeviceTestResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for JavaPOS device discovery and testing.
 * Provides endpoints to scan configured devices, test connectivity,
 * and generate workstation configuration files.
 */
@RestController
@Tag(name = "Device Discovery")
@RequestMapping("/v1/discovery")
public class DeviceDiscoveryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceDiscoveryController.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("Common", "DeviceDiscoveryController", LOGGER);

    private final JavaPOSDeviceDiscoveryService discoveryService;

    @Autowired
    public DeviceDiscoveryController(JavaPOSDeviceDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    /**
     * Scan all configured devices from devcon.xml (fast, no hardware interaction).
     */
    @Operation(description = "Scan all configured devices from the JavaPOS registry (devcon.xml)")
    @GetMapping
    public ResponseEntity<DeviceDiscoveryResponse> scanDevices() {
        log.successAPI("API Request Received", 1, "/v1/discovery", null, 0);
        DeviceDiscoveryResponse response = discoveryService.scanOnly();
        return ResponseEntity.ok(response);
    }

    /**
     * Scan and test all configured devices (slow, opens each device).
     */
    @Operation(description = "Scan and test all configured devices for connectivity")
    @GetMapping("/test")
    public ResponseEntity<DeviceDiscoveryResponse> testAllDevices() {
        log.successAPI("API Request Received", 1, "/v1/discovery/test", null, 0);
        DeviceDiscoveryResponse response = discoveryService.testAllDevices();
        return ResponseEntity.ok(response);
    }

    /**
     * Test a single device by its logical name from devcon.xml.
     */
    @Operation(description = "Test a single device by logical name")
    @GetMapping("/test/{logicalName}")
    public ResponseEntity<DeviceTestResult> testDevice(
            @PathVariable String logicalName,
            @RequestParam(required = false, defaultValue = "") String category) {
        log.successAPI("API Request Received", 1, "/v1/discovery/test/" + logicalName, null, 0);

        // If category not provided, look it up from the registry
        if (category.isEmpty()) {
            var devices = discoveryService.scanConfiguredDevices();
            var match = devices.stream()
                    .filter(d -> d.getLogicalName().equals(logicalName))
                    .findFirst();
            if (match.isPresent()) {
                category = match.get().getCategory();
            } else {
                return ResponseEntity.notFound().build();
            }
        }

        DeviceTestResult result = discoveryService.testDevice(logicalName, category);
        return ResponseEntity.ok(result);
    }

    /**
     * Generate a possum-config.yml from discovered devices.
     * Runs full discovery + tests, then produces a YAML config file
     * with the first functional device for each category.
     */
    @Operation(description = "Generate a possum-config.yml from discovered and tested devices")
    @PostMapping(value = "/generate-config", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> generateConfig() {
        log.successAPI("API Request Received", 1, "/v1/discovery/generate-config", null, 0);
        String yaml = discoveryService.generateConfigYaml();
        return ResponseEntity.ok(yaml);
    }
}
