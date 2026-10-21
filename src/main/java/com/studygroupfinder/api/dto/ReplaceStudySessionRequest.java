package com.studygroupfinder.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;
import java.util.List;

public record ReplaceStudySessionRequest(
        @NotNull Long courseId,
        Long buildingId,
        @NotNull @Pattern(regexp = "IN_PERSON|REMOTE|ASYNC") String deliveryMode,
        @NotNull OffsetDateTime startTime,
        @NotNull OffsetDateTime endTime,
        @NotNull @Min(1) Integer capacity,
        String description,
        @NotEmpty List<@NotNull @Positive Long> majorIds
) {

    @AssertTrue(message = "Start time must be before end time")
    public boolean isValidTimeRange() {
        return startTime == null || endTime == null
                || startTime.toInstant().isBefore(endTime.toInstant());
    }
}
