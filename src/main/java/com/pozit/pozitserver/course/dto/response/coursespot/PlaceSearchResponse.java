package com.pozit.pozitserver.course.dto.response.coursespot;

import java.util.List;

public record PlaceSearchResponse(
        int currentCursor,
        Integer nextCursor,
        boolean hasNext,
        int size,
        List<PlaceSearchItemResponse> places
) {
}
