package com.pozit.pozitserver.travel.controller;

import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.travel.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/regions")
public class RegionController {

    private final RegionService regionService;

//    @GetMapping("/search")
//    @Operation(summary="여행 정보 입력 후 생성")
//    public SuccessResponse<List<RegionResponse>> getRegions(
//    ){
//        return SuccessResponse.ok(regionService.getRegions());
//    }

}
