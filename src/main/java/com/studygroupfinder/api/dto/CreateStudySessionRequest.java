
package com.studygroupfinder.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;
import java.util.List;

// Information needed to create a study session.
public record CreateStudySessionRequest(

        @NotNull
        Long courseId,

        // A building is optional.
        Long buildingId,

        // Allowed session types.
        @NotNull
        @Pattern(regexp = "IN_PERSON|REMOTE|ASYNC")
        String deliveryMode,

        @NotNull
        OffsetDateTime startTime,

        @NotNull
        OffsetDateTime endTime,

        // At least one participant must be allowed.
        @NotNull
        @Min(1)
        Integer capacity,

        String description,

        // Majors selected for this session.
        @NotEmpty
        List<@NotNull @Positive Long> majorIds
) {

        // Supports tests that use the original request fields.
        public CreateStudySessionRequest(
                Long courseId,
                Long buildingId,
                String deliveryMode,
                OffsetDateTime startTime,
                OffsetDateTime endTime,
                Integer capacity,
                String description
        ) {
                this(
                        courseId,
                        buildingId,
                        deliveryMode,
                        startTime,
                        endTime,
                        capacity,
                        description,
                        List.of()
                );
        }

        // The start time must be earlier than the end time.
        @AssertTrue(message = "Start time must be before end time")
        public boolean isValidTimeRange() {

                // Other annotations check missing times.
                if (startTime == null || endTime == null) {
                        return true;
                }

                return startTime.toInstant().isBefore(endTime.toInstant());
        }
}
