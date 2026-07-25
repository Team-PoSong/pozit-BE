package com.pozit.pozitserver.travel.dto.response;

import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.travel.domain.TravelMember;

import java.util.List;

public record JoinResponse(
        Long travelId,
        Long travelMemberId
){
    public static JoinResponse from(
            Travel travel,
            TravelMember travelMember
    ){
        return new JoinResponse(
                travel.getId(),
                travelMember.getId()
        );
    }
}
