
package com.studygroupfinder.api.repository;

import com.studygroupfinder.api.entity.StudySession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from StudySession session where session.sessionId = :sessionId")
    Optional<StudySession> findByIdForUpdate(@Param("sessionId") Long sessionId);
}
