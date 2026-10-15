
package com.studygroupfinder.api.repository;

import com.studygroupfinder.api.entity.SessionMajor;
import com.studygroupfinder.api.entity.SessionMajorId;
import org.springframework.data.jpa.repository.JpaRepository;

// Provides database operations for the SESSION_MAJOR join table.
// Each record connects one study session to one target major.
public interface SessionMajorRepository
        extends JpaRepository<SessionMajor, SessionMajorId> {

    // Spring Data JPA automatically provides save(), findById(),
    // existsById(), and other standard database operations.
    // No custom queries are needed for basic inserts.
}
