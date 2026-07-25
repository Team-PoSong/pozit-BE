package com.pozit.pozitserver.travel.dto.response;

import com.pozit.pozitserver.travel.domain.Travel;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "여행 생성 응답")
public record TravelCreateResponse (
        @Schema(description = "생성된 여행 ID", example = "1")
        Long travelId
){
    public static TravelCreateResponse from(Travel travel){
        return new TravelCreateResponse(
                travel.getId()
        );
    }
}
