
package com.studygroupfinder.api.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// Represents one major targeted by a study session.
// The SESSION_MAJOR table connects study sessions with majors.
@Entity
@Table(name = "SESSION_MAJOR")
public class SessionMajor {

    // Combines session_id and major_id into one composite primary key.
    // This prevents the same major from being linked to the
    // same study session more than once.
    @EmbeddedId
    private SessionMajorId id;

    // JPA requires a no-argument constructor to load database records.
    public SessionMajor() {
    }

    // Creates a relationship between an existing session and a major.
    public SessionMajor(Long sessionId, Long majorId) {
        this.id = new SessionMajorId(sessionId, majorId);
    }

    // Returns the composite key for this relationship.
    public SessionMajorId getId() {
        return id;
    }

    // Allows JPA to set the composite key when necessary.
    public void setId(SessionMajorId id) {
        this.id = id;
    }
}
