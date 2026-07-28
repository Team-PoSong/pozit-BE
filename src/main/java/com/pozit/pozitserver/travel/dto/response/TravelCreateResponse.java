package com.pozit.pozitserver.travel.dto.response;

import com.pozit.pozitserver.course.domain.Course;
import com.pozit.pozitserver.travel.domain.Travel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "여행 생성 응답")
public record TravelCreateResponse (
        @Schema(description = "생성된 여행 ID", example = "1")
        Long travelId,

        @Schema(description = "생성된 일자별 코스 목록")
        List<CourseInfo> courses
){
    public static TravelCreateResponse from(
            Travel travel,
            List<Course> courses
    ){
        return new TravelCreateResponse(
                travel.getId(),
                courses.stream()
                        .map(CourseInfo::from)
                        .toList()
        );
    }

    @Schema(description = "생성된 일자별 코스 정보")
    public record CourseInfo(
            @Schema(description = "코스 ID", example = "1")
            Long courseId,

            @Schema(description = "여행 일차", example = "1")
            Integer dayNumber,

            @Schema(description = "코스 날짜", example = "2026-08-01")
            LocalDate date
    ) {
        public static CourseInfo from(Course course) {
            return new CourseInfo(
                    course.getId(),
                    course.getDayNumber(),
                    course.getDate()
            );
        }
    }
}
