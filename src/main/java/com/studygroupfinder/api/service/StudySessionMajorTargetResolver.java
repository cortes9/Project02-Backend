
package com.studygroupfinder.api.service;

import com.studygroupfinder.api.dto.CreateStudySessionRequest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;

// Resolves the majors selected by the student for a study session.
@Service
public class StudySessionMajorTargetResolver {

    public List<Long> resolveMajorIds(
            CreateStudySessionRequest request,
            Long creatorId
    ) {

        // A valid creation request must identify the session creator.
        if (creatorId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated creator is required"
            );
        }

        // Major selections come from the submitted session request,
        // not from the creator's own major.
        if (request == null
                || request.majorIds() == null
                || request.majorIds().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Select at least one major"
            );
        }

        List<Long> majorIds = request.majorIds();

        // Reject null identifiers before querying the database.
        if (majorIds.stream().anyMatch(id -> id == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Major IDs cannot be null"
            );
        }

        // Every selected major must appear only once.
        if (new HashSet<>(majorIds).size() != majorIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Duplicate majors are not allowed"
            );
        }

        // Return a separate list so downstream code uses a stable
        // copy of the validated major selections.
        return List.copyOf(majorIds);
    }
}
