package com.pozit.pozitserver.travel.dto.response;

import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.travel.domain.TravelStatus;

import java.time.LocalDate;
import java.util.List;

public record TravelJoinResponse(
        String message,
        InviteStatus inviteStatus,
        TravelStatus travelStatus,
        Long travelId,
        String title,
        String destination,
        String leader,
        Long memberCount,
        List<String> tags,
        LocalDate startDate,
        LocalDate endDate,
        String backgroundImageUrl,
        String imageUrl
){
    public static TravelJoinResponse from(
            Travel travel,
            String leaderNickname,
            Long memberCount,
            List<String> tags,
            String imageUrl
    ){
        return of(
                "성공적으로 조회했어요.",
                InviteStatus.JOINABLE,
                travel,
                leaderNickname,
                memberCount,
                tags,
                imageUrl
        );
    }

    public static TravelJoinResponse joined(
            Travel travel,
            String leaderNickname,
            Long memberCount,
            List<String> tags,
            String imageUrl
    ){
        return of(
                "이미 참여한 여행입니다.",
                InviteStatus.ALREADY_JOINED,
                travel,
                leaderNickname,
                memberCount,
                tags,
                imageUrl
        );
    }

    public static TravelJoinResponse doneTravel(
            Travel travel,
            String leaderNickname,
            Long memberCount,
            List<String> tags,
            String imageUrl
    ){
        return of(
                "참여할 수 없는 여행입니다.",
                InviteStatus.UNAVAILABLE,
                travel,
                leaderNickname,
                memberCount,
                tags,
                imageUrl
        );
    }

    private static TravelJoinResponse of(
            String message,
            InviteStatus inviteStatus,
            Travel travel,
            String leaderNickname,
            Long memberCount,
            List<String> tags,
            String imageUrl
    ) {
        return new TravelJoinResponse(
                message,
                inviteStatus,
                travel.getStatus(),
                travel.getId(),
                travel.getTitle(),
                travel.getDestination(),
                leaderNickname,
                memberCount,
                tags,
                travel.getStartDate(),
                travel.getEndDate(),
                imageUrl,
                imageUrl
        );
    }
}
