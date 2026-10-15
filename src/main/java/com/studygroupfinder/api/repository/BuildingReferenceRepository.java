
package com.studygroupfinder.api.repository;

import com.studygroupfinder.api.entity.BuildingReference;
import org.springframework.data.jpa.repository.JpaRepository;

// Provides existsById() to check optional building references.
public interface BuildingReferenceRepository
        extends JpaRepository<BuildingReference, Long> {
}
