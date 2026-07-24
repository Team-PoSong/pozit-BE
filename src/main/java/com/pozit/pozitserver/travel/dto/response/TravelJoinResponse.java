package com.pozit.pozitserver.travel.dto.response;

import com.pozit.pozitserver.travel.domain.Travel;

import java.time.LocalDate;
import java.util.List;

public record TravelJoinResponse (
        Long travelId,
        String title,
        String destination,
        String leader,
        Long memberCount,
        List<String> tags,
        LocalDate startDate,
        LocalDate endDate
){
    public static TravelJoinResponse from(
            Travel travel,
            Long memberCount,
            List<String> tags
    ){
        return new TravelJoinResponse(
                travel.getId(),
                travel.getTitle(),
                travel.getDestination(),
                travel.getLeader().getNickname(),
                memberCount,
                tags,
                travel.getStartDate(),
                travel.getEndDate()
        );
    }
}
