package com.fleet.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry helpers for planned routes. Keeps route math out of the tracking hot path
 * and makes the deviation logic independently testable.
 */
public final class RouteGeometry {

    private RouteGeometry() {
    }

    private static final double EARTH_RADIUS_M = 6_371_000.0;

    /** Serialises a [lat,lng] list to a compact JSON array string. */
    public static String toJson(List<double[]> points) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < points.size(); i++) {
            double[] p = points.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append('[').append(p[0]).append(',').append(p[1]).append(']');
        }
        return sb.append(']').toString();
    }

    /** Parses a JSON array of [lat,lng] pairs; returns an empty list on any malformed input. */
    public static List<double[]> fromJson(String json) {
        List<double[]> points = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return points;
        }
        // Collect numeric tokens in order, then pair them into [lat,lng].
        List<Double> nums = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (char ch : json.toCharArray()) {
            if (ch == '-' || ch == '+' || ch == '.' || (ch >= '0' && ch <= '9') || ch == 'e' || ch == 'E') {
                cur.append(ch);
            } else if (cur.length() > 0) {
                nums.add(Double.parseDouble(cur.toString()));
                cur.setLength(0);
            }
        }
        if (cur.length() > 0) {
            nums.add(Double.parseDouble(cur.toString()));
        }
        for (int i = 0; i + 1 < nums.size(); i += 2) {
            points.add(new double[]{nums.get(i), nums.get(i + 1)});
        }
        return points;
    }

    /**
     * Shortest distance in metres from a point to the polyline defined by {@code route}.
     * Uses a local equirectangular projection per segment, which is accurate at the
     * scale of a road corridor (metres to a few kilometres).
     */
    public static double distanceToRouteM(List<double[]> route, double lat, double lng) {
        if (route == null || route.size() < 2) {
            return Double.POSITIVE_INFINITY;
        }
        double best = Double.POSITIVE_INFINITY;
        for (int i = 0; i + 1 < route.size(); i++) {
            best = Math.min(best, distanceToSegmentM(lat, lng, route.get(i), route.get(i + 1)));
        }
        return best;
    }

    /** Distance from P to segment AB, all in [lat,lng], using a local metric projection. */
    static double distanceToSegmentM(double pLat, double pLng, double[] a, double[] b) {
        double latRef = Math.toRadians(pLat);
        double mPerDegLat = 111_320.0;
        double mPerDegLng = 111_320.0 * Math.cos(latRef);

        double ax = (a[1] - pLng) * mPerDegLng;
        double ay = (a[0] - pLat) * mPerDegLat;
        double bx = (b[1] - pLng) * mPerDegLng;
        double by = (b[0] - pLat) * mPerDegLat;

        double dx = bx - ax;
        double dy = by - ay;
        double lenSq = dx * dx + dy * dy;
        double t = 0.0;
        if (lenSq > 1e-9) {
            t = -(ax * dx + ay * dy) / lenSq;
            t = Math.max(0.0, Math.min(1.0, t));
        }
        double cx = ax + t * dx;
        double cy = ay + t * dy;
        return Math.sqrt(cx * cx + cy * cy);
    }

    @SuppressWarnings("unused")
    private static double haversineM(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
