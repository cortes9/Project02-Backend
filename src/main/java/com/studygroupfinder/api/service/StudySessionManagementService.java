package com.studygroupfinder.api.service;

import com.studygroupfinder.api.dto.ReplaceStudySessionRequest;
import com.studygroupfinder.api.dto.UpdateStudySessionRequest;
import com.studygroupfinder.api.entity.SessionMajor;
import com.studygroupfinder.api.entity.SessionMember;
import com.studygroupfinder.api.entity.SessionMemberId;
import com.studygroupfinder.api.entity.StudySession;
import com.studygroupfinder.api.entity.User;
import com.studygroupfinder.api.error.ConflictException;
import com.studygroupfinder.api.error.ResourceNotFoundException;
import com.studygroupfinder.api.repository.SessionMajorRepository;
import com.studygroupfinder.api.repository.SessionMemberRepository;
import com.studygroupfinder.api.repository.StudySessionRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class StudySessionManagementService {

    private static final Set<String> DELIVERY_MODES = Set.of(
            "IN_PERSON", "REMOTE", "ASYNC");

    private final StudySessionRepository sessionRepository;
    private final SessionMemberRepository memberRepository;
    private final SessionMajorRepository majorLinkRepository;
    private final StudySessionReferenceValidator referenceValidator;

    public StudySessionManagementService(
            StudySessionRepository sessionRepository,
            SessionMemberRepository memberRepository,
            SessionMajorRepository majorLinkRepository,
            StudySessionReferenceValidator referenceValidator) {
        this.sessionRepository = sessionRepository;
        this.memberRepository = memberRepository;
        this.majorLinkRepository = majorLinkRepository;
        this.referenceValidator = referenceValidator;
    }

    @Transactional
    public void join(Long sessionId, User user) {
        StudySession session = lockedSession(sessionId);
        SessionMemberId membershipId = new SessionMemberId(user.getUserId(), sessionId);

        if (memberRepository.existsById(membershipId)) {
            throw new ConflictException("You have already joined this study session");
        }

        long memberCount = memberRepository.countByIdSessionId(sessionId);
        if (memberCount >= session.getCapacity()) {
            session.setStatus("FULL");
            throw new ConflictException("The study session is full");
        }

        List<Long> conflicts = memberRepository.findConflictingSessionIds(
                user.getUserId(),
                sessionId,
                session.getStartTime(),
                session.getEndTime());
        if (!conflicts.isEmpty()) {
            throw new ConflictException(
                    "This study session conflicts with joined session " + conflicts.getFirst());
        }

        memberRepository.save(new SessionMember(user.getUserId(), sessionId));
        session.setStatus(memberCount + 1 >= session.getCapacity() ? "FULL" : "OPEN");
    }

    @Transactional
    public void leave(Long sessionId, User user) {
        StudySession session = lockedSession(sessionId);
        SessionMemberId membershipId = new SessionMemberId(user.getUserId(), sessionId);
        if (memberRepository.existsById(membershipId)) {
            memberRepository.deleteById(membershipId);
        }

        long memberCount = memberRepository.countByIdSessionId(sessionId);
        if (memberCount < session.getCapacity()) {
            session.setStatus("OPEN");
        }
    }

    @Transactional
    public Long replace(Long sessionId, User user, ReplaceStudySessionRequest request) {
        StudySession session = lockedSession(sessionId);
        requireCreator(session, user);
        applyAndValidate(
                session,
                request.courseId(),
                request.buildingId(),
                request.deliveryMode(),
                request.startTime(),
                request.endTime(),
                request.capacity(),
                request.description(),
                request.majorIds());
        replaceMajors(sessionId, request.majorIds());
        return sessionId;
    }

    @Transactional
    public Long update(Long sessionId, User user, UpdateStudySessionRequest request) {
        StudySession session = lockedSession(sessionId);
        requireCreator(session, user);

        Long courseId = request.isCourseIdSet() ? request.getCourseId() : session.getCourseId();
        Long buildingId = request.isBuildingIdSet() ? request.getBuildingId() : session.getBuildingId();
        String deliveryMode = request.isDeliveryModeSet()
                ? request.getDeliveryMode() : session.getDeliveryMode();
        OffsetDateTime startTime = request.isStartTimeSet()
                ? request.getStartTime() : session.getStartTime();
        OffsetDateTime endTime = request.isEndTimeSet()
                ? request.getEndTime() : session.getEndTime();
        Integer capacity = request.isCapacitySet() ? request.getCapacity() : session.getCapacity();
        String description = request.isDescriptionSet()
                ? request.getDescription() : session.getDescription();
        List<Long> majorIds = request.isMajorIdsSet()
                ? request.getMajorIds()
                : majorLinkRepository.findByIdSessionIdOrderByIdMajorId(sessionId).stream()
                        .map(link -> link.getId().getMajorId())
                        .toList();

        applyAndValidate(
                session,
                courseId,
                buildingId,
                deliveryMode,
                startTime,
                endTime,
                capacity,
                description,
                majorIds);
        if (request.isMajorIdsSet()) {
            replaceMajors(sessionId, majorIds);
        }
        return sessionId;
    }

    @Transactional
    public void delete(Long sessionId, User user) {
        StudySession session = lockedSession(sessionId);
        if (!session.getCreatorId().equals(user.getUserId()) && !user.isAdmin()) {
            throw new AccessDeniedException("Only the creator or an administrator may delete this session");
        }
        memberRepository.deleteByIdSessionId(sessionId);
        majorLinkRepository.deleteByIdSessionId(sessionId);
        sessionRepository.delete(session);
    }

    private StudySession lockedSession(Long sessionId) {
        return sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Study session does not exist"));
    }

    private void requireCreator(StudySession session, User user) {
        if (!session.getCreatorId().equals(user.getUserId())) {
            throw new AccessDeniedException("Only the creator may update this session");
        }
    }

    private void applyAndValidate(
            StudySession session,
            Long courseId,
            Long buildingId,
            String deliveryMode,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            Integer capacity,
            String description,
            List<Long> majorIds) {

        if (deliveryMode == null || !DELIVERY_MODES.contains(deliveryMode)) {
            throw new IllegalArgumentException("Delivery mode is invalid");
        }
        if (startTime == null || endTime == null
                || !startTime.toInstant().isBefore(endTime.toInstant())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        if (capacity == null || capacity < 1) {
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }
        validateMajors(majorIds);
        referenceValidator.validateReferences(courseId, buildingId, majorIds);

        long memberCount = memberRepository.countByIdSessionId(session.getSessionId());
        if (capacity < memberCount) {
            throw new ConflictException("Capacity cannot be lower than the current member count");
        }

        session.setCourseId(courseId);
        session.setBuildingId(buildingId);
        session.setDeliveryMode(deliveryMode);
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        session.setCapacity(capacity);
        session.setDescription(description);
        session.setStatus(memberCount >= capacity ? "FULL" : "OPEN");
    }

    private void validateMajors(List<Long> majorIds) {
        if (majorIds == null || majorIds.isEmpty()) {
            throw new IllegalArgumentException("At least one major target is required");
        }
        Set<Long> uniqueMajorIds = new HashSet<>(majorIds);
        if (uniqueMajorIds.contains(null) || uniqueMajorIds.size() != majorIds.size()) {
            throw new IllegalArgumentException("Major targets must be unique valid identifiers");
        }
    }

    private void replaceMajors(Long sessionId, List<Long> majorIds) {
        majorLinkRepository.deleteByIdSessionId(sessionId);
        for (Long majorId : majorIds) {
            majorLinkRepository.save(new SessionMajor(sessionId, majorId));
        }
    }
}
