package com.fleet.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Minimal, dependency-free HS256 JWT implementation.
 * Avoids third-party JWT libraries so the project builds/runs with zero extra setup.
 */
@Service
public class JwtService {

    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public record Claims(Long userId, String username, String role, Long companyId) {
    }

    public String generate(Long userId, String username, String role, Long companyId) {
        String header = b64(HEADER.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis() / 1000;
        long exp = now + (expirationMs / 1000);
        String payloadJson = "{"
                + "\"sub\":" + userId + ","
                + "\"username\":\"" + esc(username) + "\","
                + "\"role\":\"" + esc(role) + "\","
                + "\"companyId\":" + (companyId == null ? "null" : companyId) + ","
                + "\"iat\":" + now + ","
                + "\"exp\":" + exp + "}";
        String payload = b64(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = sign(header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    /** Returns null when the token is invalid or expired. */
    public Claims parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String expected = sign(parts[0] + "." + parts[1]);
            if (!expected.equals(parts[2])) {
                return null;
            }
            String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Long exp = num(json, "exp");
            if (exp != null && exp < System.currentTimeMillis() / 1000) {
                return null;
            }
            return new Claims(num(json, "sub"), str(json, "username"), str(json, "role"), num(json, "companyId"));
        } catch (Exception ex) {
            return null;
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return b64(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign JWT", ex);
        }
    }

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String esc(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String str(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"").matcher(json);
        return m.find() ? m.group(1).replace("\\\"", "\"").replace("\\\\", "\\") : null;
    }

    private static Long num(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+|null)").matcher(json);
        if (m.find()) {
            String v = m.group(1);
            return "null".equals(v) ? null : Long.parseLong(v);
        }
        return null;
    }
}
