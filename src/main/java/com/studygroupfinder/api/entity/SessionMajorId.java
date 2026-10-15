
package com.studygroupfinder.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

// Represents the composite primary key for SESSION_MAJOR.
// Each session-major combination must be unique.
@Embeddable
public class SessionMajorId implements Serializable {

    // Identifies the study session.
    @Column(name = "session_id")
    private Long sessionId;

    // Identifies the major targeted by the session.
    @Column(name = "major_id")
    private Long majorId;

    // Required by JPA when loading records from the database.
    public SessionMajorId() {
    }

    // Creates a composite key from a session ID and major ID.
    public SessionMajorId(Long sessionId, Long majorId) {
        this.sessionId = sessionId;
        this.majorId = majorId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public Long getMajorId() {
        return majorId;
    }

    // JPA uses equality to identify records with the same composite key.
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof SessionMajorId other)) {
            return false;
        }

        return Objects.equals(sessionId, other.sessionId)
                && Objects.equals(majorId, other.majorId);
    }

    // Equal keys must have the same hash code.
    @Override
    public int hashCode() {
        return Objects.hash(sessionId, majorId);
    }
}
