package com.pozit.pozitserver.travel.dto.response;

import com.pozit.pozitserver.travel.domain.Travel;

import java.time.LocalDate;
import java.util.List;

public record TravelJoinResponse(
        String message,
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
                "성공적으로 조회했어요.",
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

    public static TravelJoinResponse joined(
            Travel travel,
            Long memberCount,
            List<String> tags
    ){
        return new TravelJoinResponse(
                "이미 참여한 여행입니다.",
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

    public static TravelJoinResponse doneTravel(
            Travel travel,
            Long memberCount,
            List<String> tags
    ){
        return new TravelJoinResponse(
                "참여할 수 없는 여행입니다.",
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

