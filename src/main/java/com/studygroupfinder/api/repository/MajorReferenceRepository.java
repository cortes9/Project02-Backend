
package com.studygroupfinder.api.repository;

import com.studygroupfinder.api.entity.MajorReference;
import org.springframework.data.jpa.repository.JpaRepository;

// Provides existsById() to check major references.
public interface MajorReferenceRepository
        extends JpaRepository<MajorReference, Long> {
}
