
package com.studygroupfinder.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Represents an existing student account in the USER table.
// Issue #9 only needs this entity to identify the session creator.
// This class does not create Google accounts or handle OAuth login.
@Entity
@Table(name = "\"USER\"")
public class User {

    // Unique database ID used as STUDY_SESSION.creator_id.
    @Id
    @Column(name = "user_id")
    private Long userId;

    // Unique identity assigned by the authentication provider.
    // The backend must obtain this from a verified authenticated token.
    @Column(name = "oauth_subject", nullable = false, unique = true)
    private String oauthSubject;

    // JPA requires a no-argument constructor.
    protected User() {
    }

    public Long getUserId() {
        return userId;
    }

    public String getOauthSubject() {
        return oauthSubject;
    }
}
