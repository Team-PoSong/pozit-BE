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
                String cat1,
                String cat2,
                String cat3,
                String overview,
                String homepage,
                String infocenter,
                String restdate,
                String usetime,
                String parking,
                String expguide,
                String chkpet,
                String eventstartdate,
                String eventenddate,
                String playtime,
                String usetimefestival,
                String discountinfofestival,
                String spendtimefestival,
                String opentimefood,
                String restdatefood,
                String parkingfood,
                String treatmenu,
                String firstmenu,
                String opentime,
                String restdateculture,
                String parkingculture,
                String infoname,
                String infotext,
                String serialnum,

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
