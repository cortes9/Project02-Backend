
package com.studygroupfinder.api.repository;

import com.studygroupfinder.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Looks up existing users without creating or modifying their profiles.
// Issue #9 uses this to identify the creator of a study session.
public interface UserRepository extends JpaRepository<User, Long> {

    // Finds the database user whose OAuth subject matches a verified JWT.
    // An empty Optional means no matching user account exists.
    Optional<User> findByOauthSubject(String oauthSubject);
}
