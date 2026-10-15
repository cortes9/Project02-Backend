
package com.studygroupfinder.api.repository;

import com.studygroupfinder.api.entity.CourseReference;
import org.springframework.data.jpa.repository.JpaRepository;

// Provides existsById() to check that a course is in the database.
public interface CourseReferenceRepository
        extends JpaRepository<CourseReference, Long> {
}
