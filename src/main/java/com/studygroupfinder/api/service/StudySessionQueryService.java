package com.studygroupfinder.api.service;

import com.studygroupfinder.api.dto.StudySessionPageResponse;
import com.studygroupfinder.api.dto.StudySessionResponse;
import com.studygroupfinder.api.entity.MajorReference;
import com.studygroupfinder.api.entity.SessionMajor;
import com.studygroupfinder.api.entity.StudySession;
import com.studygroupfinder.api.error.ResourceNotFoundException;
import com.studygroupfinder.api.repository.MajorReferenceRepository;
import com.studygroupfinder.api.repository.SessionMajorRepository;
import com.studygroupfinder.api.repository.SessionMemberRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudySessionQueryService {

    private static final int MAX_LIMIT = 100;

    private final EntityManager entityManager;
    private final SessionMemberRepository memberRepository;
    private final SessionMajorRepository sessionMajorRepository;
    private final MajorReferenceRepository majorRepository;

    public StudySessionQueryService(
            EntityManager entityManager,
            SessionMemberRepository memberRepository,
            SessionMajorRepository sessionMajorRepository,
            MajorReferenceRepository majorRepository) {
        this.entityManager = entityManager;
        this.memberRepository = memberRepository;
        this.sessionMajorRepository = sessionMajorRepository;
        this.majorRepository = majorRepository;
    }

    @Transactional(readOnly = true)
    public StudySessionPageResponse findSessions(
            String courseCode,
            Long buildingId,
            LocalDate date,
            int limit,
            int offset) {

        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("Limit must be between 1 and 100");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be zero or greater");
        }

        QueryParts parts = queryParts(courseCode, buildingId, date);
        TypedQuery<StudySession> query = entityManager.createQuery(
                "select session from StudySession session "
                        + "join fetch session.course course "
                        + "left join fetch session.building building "
                        + "join fetch session.creator creator "
                        + parts.whereClause()
                        + " order by session.startTime, session.sessionId",
                StudySession.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(
                "select count(session) from StudySession session "
                        + "join session.course course "
                        + parts.whereClause(),
                Long.class);
        parts.parameters().forEach((name, value) -> {
            query.setParameter(name, value);
            countQuery.setParameter(name, value);
        });

        List<StudySession> sessions = query
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
        long total = countQuery.getSingleResult();
        List<StudySessionResponse> content = responsesFor(sessions);
        boolean hasNext = (long) offset + content.size() < total;

        return new StudySessionPageResponse(
                content,
                limit,
                offset,
                total,
                hasNext,
                hasNext ? offset + content.size() : null);
    }

    @Transactional(readOnly = true)
    public StudySessionResponse findSession(Long sessionId) {
        List<StudySession> sessions = entityManager.createQuery(
                        "select session from StudySession session "
                                + "join fetch session.course "
                                + "left join fetch session.building "
                                + "join fetch session.creator "
                                + "where session.sessionId = :sessionId",
                        StudySession.class)
                .setParameter("sessionId", sessionId)
                .getResultList();

        if (sessions.isEmpty()) {
            throw new ResourceNotFoundException("Study session does not exist");
        }
        return responsesFor(sessions).getFirst();
    }

    private QueryParts queryParts(String courseCode, Long buildingId, LocalDate date) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> parameters = new LinkedHashMap<>();

        if (courseCode != null && !courseCode.isBlank()) {
            conditions.add("upper(course.courseCode) = :courseCode");
            parameters.put("courseCode", courseCode.trim().toUpperCase());
        }
        if (buildingId != null) {
            conditions.add("session.buildingId = :buildingId");
            parameters.put("buildingId", buildingId);
        }
        if (date != null) {
            OffsetDateTime dateStart = date.atStartOfDay().atOffset(ZoneOffset.UTC);
            conditions.add("session.startTime < :dateEnd and session.endTime > :dateStart");
            parameters.put("dateStart", dateStart);
            parameters.put("dateEnd", dateStart.plusDays(1));
        }

        String where = conditions.isEmpty()
                ? ""
                : "where " + String.join(" and ", conditions);
        return new QueryParts(where, parameters);
    }

    private List<StudySessionResponse> responsesFor(List<StudySession> sessions) {
        if (sessions.isEmpty()) {
            return List.of();
        }

        List<Long> sessionIds = sessions.stream().map(StudySession::getSessionId).toList();
        Map<Long, Long> memberCounts = new HashMap<>();
        for (Object[] result : memberRepository.countMembersBySessionIds(sessionIds)) {
            memberCounts.put((Long) result[0], (Long) result[1]);
        }

        List<SessionMajor> links = sessionMajorRepository
                .findByIdSessionIdInOrderByIdSessionIdAscIdMajorIdAsc(sessionIds);
        List<Long> majorIds = links.stream()
                .map(link -> link.getId().getMajorId())
                .distinct()
                .toList();
        Map<Long, MajorReference> majorsById = new HashMap<>();
        majorRepository.findAllById(majorIds)
                .forEach(major -> majorsById.put(major.getMajorId(), major));
        Map<Long, List<StudySessionResponse.MajorSummary>> majorsBySession = new HashMap<>();
        for (SessionMajor link : links) {
            MajorReference major = majorsById.get(link.getId().getMajorId());
            majorsBySession.computeIfAbsent(link.getId().getSessionId(), ignored -> new ArrayList<>())
                    .add(new StudySessionResponse.MajorSummary(
                            link.getId().getMajorId(),
                            major == null ? null : major.getMajorName()));
        }

        return sessions.stream()
                .map(session -> toResponse(
                        session,
                        memberCounts.getOrDefault(session.getSessionId(), 0L),
                        majorsBySession.getOrDefault(session.getSessionId(), List.of())))
                .toList();
    }

    private StudySessionResponse toResponse(
            StudySession session,
            long memberCount,
            Collection<StudySessionResponse.MajorSummary> majors) {
        return new StudySessionResponse(
                session.getSessionId(),
                session.getCreatorId(),
                session.getCourseId(),
                session.getBuildingId(),
                session.getDeliveryMode(),
                session.getStartTime(),
                session.getEndTime(),
                session.getCapacity(),
                session.getDescription(),
                session.getStatus(),
                Math.toIntExact(memberCount),
                new StudySessionResponse.CourseSummary(
                        session.getCourseId(),
                        session.getCourse().getCourseCode(),
                        session.getCourse().getCourseName()),
                session.getBuilding() == null
                        ? null
                        : new StudySessionResponse.BuildingSummary(
                                session.getBuildingId(),
                                session.getBuilding().getBuildingName()),
                new StudySessionResponse.CreatorSummary(
                        session.getCreatorId(),
                        session.getCreator().getName()),
                List.copyOf(majors));
    }

    private record QueryParts(String whereClause, Map<String, Object> parameters) {
    }
}
