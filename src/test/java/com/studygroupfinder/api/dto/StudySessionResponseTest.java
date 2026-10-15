
package com.studygroupfinder.api.dto;

import com.studygroupfinder.api.entity.StudySession;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class StudySessionResponseTest {

    @Test
    void convertsSavedSessionIntoResponse() {

        // Create an entity representing a newly saved study session.
        StudySession session = new StudySession();

        session.setCreatorId(42L);
        session.setCourseId(10L);
        session.setBuildingId(5L);
        session.setDeliveryMode("IN_PERSON");
        session.setStartTime(
                OffsetDateTime.parse("2026-10-15T14:00:00Z")
        );
        session.setEndTime(
                OffsetDateTime.parse("2026-10-15T16:00:00Z")
        );
        session.setCapacity(12);
        session.setDescription("Exam preparation");
        session.setStatus("OPEN");

        // Convert the database entity into an API response.
        StudySessionResponse response =
                StudySessionResponse.fromEntity(session);

        // Check that the response preserves the session's information.
        assertEquals(42L, response.creatorId());
        assertEquals(10L, response.courseId());
        assertEquals(5L, response.buildingId());
        assertEquals("IN_PERSON", response.deliveryMode());
        assertEquals(12, response.capacity());
        assertEquals("Exam preparation", response.description());
        assertEquals("OPEN", response.status());

        // A newly created session begins with no participants.
        assertEquals(0, response.participantCount());
    }

    @Test
    void allowsSessionWithoutBuilding() {

        // Remote sessions may not have a physical building.
        StudySession session = new StudySession();
        session.setBuildingId(null);

        StudySessionResponse response =
                StudySessionResponse.fromEntity(session);

        assertNull(response.buildingId());
        assertEquals(0, response.participantCount());
    }
}
