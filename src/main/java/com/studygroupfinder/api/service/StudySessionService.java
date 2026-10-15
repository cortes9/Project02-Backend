
package com.studygroupfinder.api.service;

import com.studygroupfinder.api.dto.CreateStudySessionRequest;
import org.springframework.stereotype.Service;

@Service
public class StudySessionService {

    // Checks the study session's time range before it can be saved.
    // A session must start strictly before its end time.
    public void validateSessionTime(CreateStudySessionRequest request) {

        // Required timestamps are also checked by DTO Bean Validation.
        if (request.startTime() == null || request.endTime() == null) {
            throw new IllegalArgumentException(
                    "Start time and end time are required"
            );
        }

        // Reject sessions whose start time is equal to or after the end time.
        if (!request.startTime().toInstant()
                .isBefore(request.endTime().toInstant())) {

            throw new IllegalArgumentException(
                    "Start time must be before end time"
            );
        }
    }
}
