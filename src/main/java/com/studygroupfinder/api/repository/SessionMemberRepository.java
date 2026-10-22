package com.studygroupfinder.api.repository;

import com.studygroupfinder.api.entity.SessionMember;
import com.studygroupfinder.api.entity.SessionMemberId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface SessionMemberRepository
        extends JpaRepository<SessionMember, SessionMemberId> {

    long countByIdSessionId(Long sessionId);

    void deleteByIdSessionId(Long sessionId);

    @Query("""
            select session.sessionId
            from StudySession session, SessionMember member
            where member.id.userId = :userId
              and member.id.sessionId = session.sessionId
              and session.sessionId <> :candidateSessionId
              and session.startTime < :candidateEnd
              and session.endTime > :candidateStart
            order by session.startTime, session.sessionId
            """)
    List<Long> findConflictingSessionIds(
            @Param("userId") Long userId,
            @Param("candidateSessionId") Long candidateSessionId,
            @Param("candidateStart") OffsetDateTime candidateStart,
            @Param("candidateEnd") OffsetDateTime candidateEnd);

    @Query("""
            select member.id.sessionId, count(member)
            from SessionMember member
            where member.id.sessionId in :sessionIds
            group by member.id.sessionId
            """)
    List<Object[]> countMembersBySessionIds(
            @Param("sessionIds") Collection<Long> sessionIds);
}
