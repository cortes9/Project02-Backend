
package com.studygroupfinder.api.dto;

import com.studygroupfinder.api.entity.StudySession;

import java.time.OffsetDateTime;
import java.util.List;

// Represents the study session information returned by the API.
public record StudySessionResponse(

        // Database-generated identifier for the session.
        Long sessionId,

        // Database identifier of the student who created the session.
        Long creatorId,

        // Course and optional building associated with the session.
        Long courseId,
        Long buildingId,

        // Session format and scheduled meeting times.
        String deliveryMode,
        OffsetDateTime startTime,
        OffsetDateTime endTime,

        // Session capacity and optional description.
        Integer capacity,
        String description,

        // Current session status and number of participants.
        String status,
        Integer participantCount,

        CourseSummary course,
        BuildingSummary building,
        CreatorSummary creator,
        List<MajorSummary> targetedMajors
) {

    public record CourseSummary(Long courseId, String code, String name) {
    }

    public record BuildingSummary(Long buildingId, String name) {
    }

    public record CreatorSummary(Long userId, String name) {
    }

    public record MajorSummary(Long majorId, String name) {
    }

    // Converts a newly saved session entity into the API response.
    // A new session begins with zero participants when creation
    // does not automatically enroll its creator.
    public static StudySessionResponse fromEntity(StudySession session) {
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
                0,
                new CourseSummary(session.getCourseId(), null, null),
                session.getBuildingId() == null
                        ? null
                        : new BuildingSummary(session.getBuildingId(), null),
                new CreatorSummary(session.getCreatorId(), null),
                List.of()
        );
    }
}
