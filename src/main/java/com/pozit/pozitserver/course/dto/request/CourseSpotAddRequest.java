package com.pozit.pozitserver.course.dto.request;

import java.util.List;

public record CourseSpotAddRequest(
        List<Long> touristSpotIds
) {}
