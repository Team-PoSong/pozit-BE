package com.pozit.pozitserver.travel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TravelJoinRequest (

        @NotBlank(message = "초대 코드를 입력해주세요.")
        @Size(min=5,max=5,message="초대 코드는 5자리여야 합니다.")
        String inviteCode
){
}
