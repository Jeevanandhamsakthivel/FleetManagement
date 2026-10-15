package com.fleet.ws;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fleet.security.JwtService;

/**
 * Broadcasts live vehicle positions to connected dashboards.
 * Connections must present a valid JWT via the {@code token} query parameter
 * (e.g. {@code /ws/tracking?token=...}); updates are only delivered to sessions
 * belonging to the same company as the vehicle.
 */
@Component
public class TrackingWebSocketHandler extends TextWebSocketHandler {

    private static final String COMPANY_ATTR = "companyId";

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final JwtService jwtService;

    public TrackingWebSocketHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long companyId = companyIdFrom(session);
        if (companyId == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Authentication required"));
            return;
        }
        session.getAttributes().put(COMPANY_ATTR, companyId);
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    /** Sends the message only to authenticated sessions of the given company. */
    public void broadcast(String json, Long companyId) {
        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (companyId != null && !companyId.equals(session.getAttributes().get(COMPANY_ATTR))) {
                continue;
            }
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                }
            } catch (Exception ignored) {
                sessions.remove(session);
            }
        }
    }

    private Long companyIdFrom(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        for (String pair : uri.getQuery().split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && "token".equals(pair.substring(0, eq))) {
                String token = java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                JwtService.Claims claims = jwtService.parse(token);
                return claims != null ? claims.companyId() : null;
            }
        }
        return null;
    }
}
