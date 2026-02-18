package com.target.devicemanager.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Fetches device configuration from PostgREST before any Spring beans are created.
 * For each device row returned for this store+lane, injects:
 *   possum.device.{deviceType}.enabled = true
 *   possum.device.{deviceType}.logicalName = {logicalName}
 *
 * Falls back to a local JSON cache file if PostgREST is unreachable.
 * If neither source is available, no properties are injected and no devices are claimed.
 */
public class DeviceConfigEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceConfigEnvironmentPostProcessor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);
    private static final String PROPERTY_SOURCE_NAME = "deviceConfig";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        String storeId = environment.getProperty("possum.store.id");
        String laneId = environment.getProperty("possum.workstation.id");
        String baseUrl = environment.getProperty("possum.postgrest.url");
        String table = environment.getProperty("possum.postgrest.table", "device_config");

        if (baseUrl == null || baseUrl.isBlank()) {
            LOGGER.info("possum.postgrest.url not configured — skipping DB device config lookup");
            return;
        }
        if (storeId == null || laneId == null) {
            LOGGER.warn("possum.store.id or possum.workstation.id not set — skipping DB device config lookup");
            return;
        }

        Map<String, String> deviceMap = fetchFromPostgrest(baseUrl, table, storeId, laneId);
        if (deviceMap == null) {
            LOGGER.warn("PostgREST unreachable — falling back to local cache");
            deviceMap = readCache();
        }

        if (deviceMap != null && !deviceMap.isEmpty()) {
            saveCache(deviceMap);
            injectProperties(environment, deviceMap);
            LOGGER.info("Device config loaded for store={} lane={}: {}", storeId, laneId, deviceMap.keySet());
        } else {
            LOGGER.info("No device config found for store={} lane={} — no devices will be claimed", storeId, laneId);
        }
    }

    private Map<String, String> fetchFromPostgrest(String baseUrl, String table,
                                                    String storeId, String laneId) {
        try {
            String url = String.format("%s/%s?store_id=eq.%s&lane_id=eq.%s",
                    baseUrl, table, storeId, laneId);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(HTTP_TIMEOUT)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(HTTP_TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOGGER.warn("PostgREST returned status {} for device config", response.statusCode());
                return null;
            }

            JsonNode rows = MAPPER.readTree(response.body());
            if (!rows.isArray() || rows.isEmpty()) {
                LOGGER.info("PostgREST returned empty result for store={} lane={}", storeId, laneId);
                return new HashMap<>();
            }

            Map<String, String> deviceMap = new HashMap<>();
            for (JsonNode row : rows) {
                String deviceType = row.get("device_type").asText();
                String logicalName = row.get("logical_name").asText();
                deviceMap.put(deviceType, logicalName);
            }
            return deviceMap;

        } catch (Exception e) {
            LOGGER.warn("Failed to fetch device config from PostgREST: {}", e.getMessage());
            return null;
        }
    }

    private void injectProperties(ConfigurableEnvironment environment, Map<String, String> deviceMap) {
        Map<String, Object> props = new HashMap<>();
        for (Map.Entry<String, String> entry : deviceMap.entrySet()) {
            String type = entry.getKey();
            String name = entry.getValue();
            props.put("possum.device." + type + ".enabled", "true");
            props.put("possum.device." + type + ".logicalName", name);
        }
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
    }

    // ---- Local cache file ----

    private Path getCachePath() {
        String possumHome = System.getenv("POSSUM_HOME");
        if (possumHome != null && !possumHome.isBlank()) {
            return Paths.get(possumHome, "config", "device-config-cache.json");
        }
        return Paths.get("device-config-cache.json");
    }

    private void saveCache(Map<String, String> deviceMap) {
        try {
            Path path = getCachePath();
            Files.createDirectories(path.getParent());
            MAPPER.writeValue(path.toFile(), deviceMap);
            LOGGER.info("Device config cache saved to {}", path);
        } catch (IOException e) {
            LOGGER.warn("Failed to save device config cache: {}", e.getMessage());
        }
    }

    private Map<String, String> readCache() {
        try {
            Path path = getCachePath();
            if (!Files.exists(path)) {
                LOGGER.info("No device config cache file at {}", path);
                return null;
            }
            Map<String, String> cached = MAPPER.readValue(path.toFile(),
                    new TypeReference<Map<String, String>>() {});
            LOGGER.info("Loaded device config from cache: {}", cached.keySet());
            return cached;
        } catch (IOException e) {
            LOGGER.warn("Failed to read device config cache: {}", e.getMessage());
            return null;
        }
    }
}
