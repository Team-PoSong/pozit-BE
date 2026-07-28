package com.pozit.pozitserver.course.dto.response.coursespot;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TourApiResponse (
        Response response
){
    public record Response(
            Header header,
            Body body
    ){
        public record Header(
                String resultCode,
                String resultMsg
        ){
        }

        public record Body(
                Items items,
                int numOfRows,
                int pageNo,
                int totalCount
        ){
        }

        public record Items(
                List<Item> item
        ){
        }

        public record Item(
                @JsonProperty("contentid")
                String contentId,

                @JsonProperty("contenttypeid")
                String contentTypeId,
                String title,
                String addr1,
                String addr2,
                String firstimage,
                String firstimage2,
                String mapx,
                String mapy,
                String tel,

                @JsonProperty("cpyrhtDivCd")
                String copyrightType,

                @JsonProperty("lDongRegnCd")
                String legalDongRegionCode,

                @JsonProperty("lDongSignguCd")
                String legalDongSigunguCode
        ){
        }
    }
}
