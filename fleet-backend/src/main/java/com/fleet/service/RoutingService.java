package com.fleet.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Resolves place names to coordinates (Nominatim) and fetches an actual road route
 * (OSRM) between them. Both services are public and require no API key. Routing is
 * strictly best-effort: if it fails we return {@code null} rather than inventing a
 * straight line, so the UI can show an honest "route unavailable" state.
 */
@Service
public class RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

    private final RestClient http;
    private final ObjectMapper mapper;

    @Value("${app.routing.enabled:true}")
    private boolean enabled;

    @Value("${app.routing.nominatim-url:https://nominatim.openstreetmap.org}")
    private String nominatimUrl;

    @Value("${app.routing.osrm-url:https://router.project-osrm.org}")
    private String osrmUrl;

    @Value("${app.routing.user-agent:fleet-management-dev}")
    private String userAgent;

    public RoutingService(ObjectMapper mapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(6).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        this.http = RestClient.builder().requestFactory(factory).build();
        this.mapper = mapper;
    }

    /** Result of a successful route lookup. Geometry is a list of [lat,lng] pairs. */
    public record Route(List<double[]> geometry, double distanceKm, String provider) {
    }

    /** Resolves a free-text place name to [lat,lng]; returns null when not found. */
    public double[] geocode(String query) {
        if (!enabled || query == null || query.isBlank()) {
            return null;
        }
        try {
            String url = nominatimUrl + "/search?format=json&limit=1&q="
                    + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            String body = http.get().uri(url).header("User-Agent", userAgent).retrieve().body(String.class);
            JsonNode root = mapper.readTree(body);
            if (root.isArray() && root.size() > 0) {
                JsonNode first = root.get(0);
                double lat = Double.parseDouble(first.get("lat").asText());
                double lng = Double.parseDouble(first.get("lon").asText());
                return new double[]{lat, lng};
            }
        } catch (Exception e) {
            log.warn("Geocoding failed for '{}': {}", query, e.getMessage());
        }
        return null;
    }

    /** Fetches a driving route between two coordinates; returns null on failure. */
    public Route route(double originLat, double originLng, double destLat, double destLng) {
        if (!enabled) {
            return null;
        }
        try {
            String url = osrmUrl + "/route/v1/driving/"
                    + originLng + "," + originLat + ";" + destLng + "," + destLat
                    + "?overview=full&geometries=geojson";
            String body = http.get().uri(url).header("User-Agent", userAgent).retrieve().body(String.class);
            JsonNode root = mapper.readTree(body);
            if (!"Ok".equals(root.path("code").asText())) {
                log.warn("OSRM returned code={} for {} -> {}", root.path("code").asText(), originLat, destLat);
                return null;
            }
            JsonNode route = root.path("routes").path(0);
            if (route.isMissingNode()) {
                return null;
            }
            JsonNode coords = route.path("geometry").path("coordinates");
            List<double[]> geometry = new ArrayList<>();
            for (JsonNode c : coords) {
                // GeoJSON is [lng,lat]; the app stores and displays [lat,lng].
                geometry.add(new double[]{c.get(1).asDouble(), c.get(0).asDouble()});
            }
            if (geometry.size() < 2) {
                return null;
            }
            return new Route(geometry, route.path("distance").asDouble() / 1000.0, "OSRM");
        } catch (Exception e) {
            log.warn("Routing failed {}/{} -> {}/{}: {}", originLat, originLng, destLat, destLng, e.getMessage());
            return null;
        }
    }
}
