
package com.studygroupfinder.api.repository;

import com.studygroupfinder.api.entity.SessionMajor;
import com.studygroupfinder.api.entity.SessionMajorId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

// Provides database operations for the SESSION_MAJOR join table.
// Each record connects one study session to one target major.
public interface SessionMajorRepository
        extends JpaRepository<SessionMajor, SessionMajorId> {

    List<SessionMajor> findByIdSessionIdOrderByIdMajorId(Long sessionId);

    List<SessionMajor> findByIdSessionIdInOrderByIdSessionIdAscIdMajorIdAsc(
            Collection<Long> sessionIds);

    void deleteByIdSessionId(Long sessionId);

    // Spring Data JPA automatically provides save(), findById(),
    // existsById(), and other standard database operations.
    // No custom queries are needed for basic inserts.
}
