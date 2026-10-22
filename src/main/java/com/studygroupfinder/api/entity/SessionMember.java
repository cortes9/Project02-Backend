package com.studygroupfinder.api.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "SESSION_MEMBER")
public class SessionMember {

    @EmbeddedId
    private SessionMemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", insertable = false, updatable = false)
    private StudySession session;

    public SessionMember() {
    }

    public SessionMember(Long userId, Long sessionId) {
        this.id = new SessionMemberId(userId, sessionId);
    }

    public SessionMemberId getId() {
        return id;
    }
}
