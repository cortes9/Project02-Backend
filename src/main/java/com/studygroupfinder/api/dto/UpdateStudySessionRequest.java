package com.studygroupfinder.api.dto;

import com.fasterxml.jackson.annotation.JsonSetter;

import java.time.OffsetDateTime;
import java.util.List;

public class UpdateStudySessionRequest {

    private Long courseId;
    private Long buildingId;
    private String deliveryMode;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private Integer capacity;
    private String description;
    private List<Long> majorIds;

    private boolean courseIdSet;
    private boolean buildingIdSet;
    private boolean deliveryModeSet;
    private boolean startTimeSet;
    private boolean endTimeSet;
    private boolean capacitySet;
    private boolean descriptionSet;
    private boolean majorIdsSet;

    @JsonSetter
    public void setCourseId(Long courseId) {
        this.courseId = courseId;
        this.courseIdSet = true;
    }

    @JsonSetter
    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
        this.buildingIdSet = true;
    }

    @JsonSetter
    public void setDeliveryMode(String deliveryMode) {
        this.deliveryMode = deliveryMode;
        this.deliveryModeSet = true;
    }

    @JsonSetter
    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
        this.startTimeSet = true;
    }

    @JsonSetter
    public void setEndTime(OffsetDateTime endTime) {
        this.endTime = endTime;
        this.endTimeSet = true;
    }

    @JsonSetter
    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
        this.capacitySet = true;
    }

    @JsonSetter
    public void setDescription(String description) {
        this.description = description;
        this.descriptionSet = true;
    }

    @JsonSetter
    public void setMajorIds(List<Long> majorIds) {
        this.majorIds = majorIds;
        this.majorIdsSet = true;
    }

    public Long getCourseId() { return courseId; }
    public Long getBuildingId() { return buildingId; }
    public String getDeliveryMode() { return deliveryMode; }
    public OffsetDateTime getStartTime() { return startTime; }
    public OffsetDateTime getEndTime() { return endTime; }
    public Integer getCapacity() { return capacity; }
    public String getDescription() { return description; }
    public List<Long> getMajorIds() { return majorIds; }

    public boolean isCourseIdSet() { return courseIdSet; }
    public boolean isBuildingIdSet() { return buildingIdSet; }
    public boolean isDeliveryModeSet() { return deliveryModeSet; }
    public boolean isStartTimeSet() { return startTimeSet; }
    public boolean isEndTimeSet() { return endTimeSet; }
    public boolean isCapacitySet() { return capacitySet; }
    public boolean isDescriptionSet() { return descriptionSet; }
    public boolean isMajorIdsSet() { return majorIdsSet; }
}
