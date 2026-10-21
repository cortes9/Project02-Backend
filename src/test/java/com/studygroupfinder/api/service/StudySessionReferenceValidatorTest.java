
package com.studygroupfinder.api.service;

import com.studygroupfinder.api.error.ResourceNotFoundException;
import com.studygroupfinder.api.repository.BuildingReferenceRepository;
import com.studygroupfinder.api.repository.CourseReferenceRepository;
import com.studygroupfinder.api.repository.MajorReferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudySessionReferenceValidatorTest {

    private CourseReferenceRepository courseRepository;
    private BuildingReferenceRepository buildingRepository;
    private MajorReferenceRepository majorRepository;
    private StudySessionReferenceValidator validator;

    @BeforeEach
    void setUp() {

        // Mock all repositories so Docker and PostgreSQL are not required.
        courseRepository = mock(CourseReferenceRepository.class);
        buildingRepository = mock(BuildingReferenceRepository.class);
        majorRepository = mock(MajorReferenceRepository.class);

        validator = new StudySessionReferenceValidator(
                courseRepository,
                buildingRepository,
                majorRepository
        );
    }

    @Test
    void acceptsExistingReferences() {

        // All provided IDs refer to existing database records.
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(buildingRepository.existsById(2L)).thenReturn(true);
        when(majorRepository.existsById(3L)).thenReturn(true);

        assertDoesNotThrow(
                () -> validator.validateReferences(1L, 2L, List.of(3L))
        );
    }

    @Test
    void acceptsNullBuilding() {

        // The building is optional when creating a study session.
        when(courseRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(
                () -> validator.validateReferences(1L, null, List.of())
        );

        verifyNoInteractions(buildingRepository);
    }

    @Test
    void rejectsMissingCourse() {

        // A session cannot refer to a course that doesn't exist.
        when(courseRepository.existsById(1L)).thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> validator.validateReferences(1L, null, List.of())
        );
    }

    @Test
    void rejectsMissingBuilding() {

        // An invalid optional building ID must still be rejected.
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(buildingRepository.existsById(2L)).thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> validator.validateReferences(1L, 2L, List.of())
        );
    }

    @Test
    void rejectsMissingMajor() {

        // Every major must exist before any session data is saved.
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(majorRepository.existsById(3L)).thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> validator.validateReferences(1L, null, List.of(3L))
        );
    }

    @Test
    void rejectsNullMajorList() {

        // Do not proceed when the major-target information is missing.
        when(courseRepository.existsById(1L)).thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateReferences(1L, null, null)
        );
    }
}
