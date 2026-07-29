package com.pozit.pozitserver.course.dto.response.coursespot;

import com.pozit.pozitserver.course.domain.CourseSpotStatus;

public record CourseSpotResponse (
        String nickname,
        String timeLapseUrl,
        String thumbnailUrl
){
}
