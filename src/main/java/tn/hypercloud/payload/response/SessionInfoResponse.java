package tn.hypercloud.payload.response;

import jakarta.servlet.http.HttpSession;
import tn.hypercloud.entity.user.User;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class SessionInfoResponse {

    private String sessionId;
    private Long userId;
    private String email;
    private String role;
    private ZonedDateTime sessionCreatedAt;
    private ZonedDateTime lastAccessedAt;
    private Integer maxInactiveIntervalSeconds;

    public static SessionInfoResponse from(HttpSession session, User user) {
        SessionInfoResponse response = new SessionInfoResponse();
        response.setSessionId(session.getId());
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setSessionCreatedAt(toZonedDateTime(session.getCreationTime()));
        response.setLastAccessedAt(toZonedDateTime(session.getLastAccessedTime()));
        response.setMaxInactiveIntervalSeconds(session.getMaxInactiveInterval());
        return response;
    }

    private static ZonedDateTime toZonedDateTime(long epochMs) {
        return Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault());
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public ZonedDateTime getSessionCreatedAt() {
        return sessionCreatedAt;
    }

    public void setSessionCreatedAt(ZonedDateTime sessionCreatedAt) {
        this.sessionCreatedAt = sessionCreatedAt;
    }

    public ZonedDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(ZonedDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public Integer getMaxInactiveIntervalSeconds() {
        return maxInactiveIntervalSeconds;
    }

    public void setMaxInactiveIntervalSeconds(Integer maxInactiveIntervalSeconds) {
        this.maxInactiveIntervalSeconds = maxInactiveIntervalSeconds;
    }
}
