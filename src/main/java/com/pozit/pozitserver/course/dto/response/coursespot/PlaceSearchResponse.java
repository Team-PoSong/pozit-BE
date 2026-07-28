package com.pozit.pozitserver.course.dto.response.coursespot;

import java.util.List;

public record PlaceSearchResponse(
        int page,
        int size,
        int totalCount,
        List<PlaceSearchItemResponse> places
) {
}