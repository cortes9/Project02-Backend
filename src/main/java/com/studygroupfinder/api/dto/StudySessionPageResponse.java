package com.studygroupfinder.api.dto;

import java.util.List;

public record StudySessionPageResponse(
        List<StudySessionResponse> content,
        int limit,
        int offset,
        long totalElements,
        boolean hasNext,
        Integer nextOffset
) {
}
