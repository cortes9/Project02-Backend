
package com.studygroupfinder.api.service;

import com.studygroupfinder.api.error.ResourceNotFoundException;
import com.studygroupfinder.api.repository.BuildingReferenceRepository;
import com.studygroupfinder.api.repository.CourseReferenceRepository;
import com.studygroupfinder.api.repository.MajorReferenceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

// Validates that all referenced records exist before a session is saved.
// This helps prevent invalid foreign-key references in the database.
@Service
public class StudySessionReferenceValidator {

    private final CourseReferenceRepository courseRepository;
    private final BuildingReferenceRepository buildingRepository;
    private final MajorReferenceRepository majorRepository;

    // Spring injects the repositories used to validate references.
    public StudySessionReferenceValidator(
            CourseReferenceRepository courseRepository,
            BuildingReferenceRepository buildingRepository,
            MajorReferenceRepository majorRepository) {

        this.courseRepository = courseRepository;
        this.buildingRepository = buildingRepository;
        this.majorRepository = majorRepository;
    }

    // Validates the required course and optional building and majors.
    public void validateReferences(
            Long courseId,
            Long buildingId,
            List<Long> majorIds) {

        // Reject a missing or nonexistent course.
        if (courseId == null || !courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Course does not exist");
        }

        // A null building is permitted for remote or asynchronous sessions.
        if (buildingId != null && !buildingRepository.existsById(buildingId)) {
            throw new ResourceNotFoundException("Building does not exist");
        }

        // Major targets must be provided by a trusted, agreed API contract.
        if (majorIds == null) {
            throw new IllegalArgumentException("Major targets are required");
        }

        // Check each referenced major before allowing any database inserts.
        for (Long majorId : majorIds) {
            if (majorId == null || !majorRepository.existsById(majorId)) {
                throw new ResourceNotFoundException("Major does not exist");
            }
        }
    }
}
