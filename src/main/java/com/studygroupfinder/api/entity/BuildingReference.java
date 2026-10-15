
package com.studygroupfinder.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Minimal entity for checking whether a building exists.
// Building management is outside Issue #9.
@Entity
@Table(name = "BUILDING")
public class BuildingReference {

    @Id
    @Column(name = "building_id")
    private Long buildingId;

    // JPA needs this constructor to load database rows.
    protected BuildingReference() {
    }

    public Long getBuildingId() {
        return buildingId;
    }
}
