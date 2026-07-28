package com.pozit.pozitserver.course.controller;

import com.pozit.pozitserver.course.dto.response.coursespot.PlaceSearchResponse;
import com.pozit.pozitserver.course.service.CourseSpotService;
import com.pozit.pozitserver.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/course-spots")
@RequiredArgsConstructor
@Tag(name = "Course API")
public class CourseSpotController {

    private final CourseSpotService courseSpotService;

    @GetMapping("/search")
    public SuccessResponse<PlaceSearchResponse> searchCourseSpots(
            @RequestParam @NotBlank @Size(max=50,min=2) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size
    ){
        return SuccessResponse.ok(courseSpotService.search(keyword,page,size));
    }
}
