package com.studygroupfinder.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SessionMemberId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id")
    private Long sessionId;

    public SessionMemberId() {
    }

    public SessionMemberId(Long userId, Long sessionId) {
        this.userId = userId;
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SessionMemberId other)) {
            return false;
        }
        return Objects.equals(userId, other.userId)
                && Objects.equals(sessionId, other.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, sessionId);
    }
}
