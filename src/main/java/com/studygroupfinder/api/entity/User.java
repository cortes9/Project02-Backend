
package com.studygroupfinder.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Represents a student account in the USER table.
@Entity
@Table(name = "\"USER\"")
public class User {

    // Unique database ID used as STUDY_SESSION.creator_id.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    // Unique identity assigned by the authentication provider (Google sub claim).
    @Column(name = "oauth_subject", nullable = false, unique = true)
    private String oauthSubject;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "student_year")
    private String studentYear;

    @Column(name = "major_id")
    private Long majorId;

    @Column(name = "is_admin", columnDefinition = "boolean default false")
    private boolean isAdmin = false;

    // JPA requires a no-argument constructor.
    protected User() {
    }

    public User(String oauthSubject, String name, String email) {
        this.oauthSubject = oauthSubject;
        this.name = name;
        this.email = email;
        this.isAdmin = false;
    }

    public Long getUserId() {
        return userId;
    }

    public String getOauthSubject() {
        return oauthSubject;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStudentYear() {
        return studentYear;
    }

    public void setStudentYear(String studentYear) {
        this.studentYear = studentYear;
    }

    public Long getMajorId() {
        return majorId;
    }

    public void setMajorId(Long majorId) {
        this.majorId = majorId;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
}

