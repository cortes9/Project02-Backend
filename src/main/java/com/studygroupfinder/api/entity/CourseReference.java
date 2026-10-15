
package com.studygroupfinder.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Minimal read-only mapping used to verify an existing course ID.
// Course creation and management belong to other features.
@Entity
@Table(name = "COURSE")
public class CourseReference {

    @Id
    @Column(name = "course_id")
    private Long courseId;

    // Required by JPA when reading existing database records.
    protected CourseReference() {
    }

    public Long getCourseId() {
        return courseId;
    }
}
