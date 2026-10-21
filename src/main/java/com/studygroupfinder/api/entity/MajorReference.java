
package com.studygroupfinder.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Minimal entity for validating majors selected for a session.
// This does not implement major management.
@Entity
@Table(name = "MAJOR")
public class MajorReference {

    @Id
    @Column(name = "major_id")
    private Long majorId;

    @Column(name = "major_name")
    private String majorName;

    // Required by JPA.
    protected MajorReference() {
    }

    public Long getMajorId() {
        return majorId;
    }

    public String getMajorName() {
        return majorName;
    }
}
