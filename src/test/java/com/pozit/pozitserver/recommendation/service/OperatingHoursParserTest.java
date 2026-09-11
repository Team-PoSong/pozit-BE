package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.recommendation.model.CandidatePlace;
import com.pozit.pozitserver.recommendation.model.OperatingHours;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class OperatingHoursParserTest {

    private final OperatingHoursParser parser = new OperatingHoursParser();

    @Test
    void parseAllowsVisitInsideOperatingHours() {
        OperatingHours operatingHours = parser.parse(place("12", "09:00~18:00", null, null, null));

        assertThat(operatingHours.canVisit(
                LocalDate.of(2026, 9, 11),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        )).isTrue();
    }

    @Test
    void parseRejectsVisitOutsideOperatingHours() {
        OperatingHours operatingHours = parser.parse(place("12", "09:00~18:00", null, null, null));

        assertThat(operatingHours.canVisit(
                LocalDate.of(2026, 9, 11),
                LocalTime.of(18, 30),
                LocalTime.of(19, 0)
        )).isFalse();
    }

    @Test
    void parseUsesFoodOpenTimeFirstForFoodPlaces() {
        OperatingHours operatingHours = parser.parse(place("39", "09:00~11:00", null, "12:00~20:00", null));

        assertThat(operatingHours.canVisit(
                LocalDate.of(2026, 9, 11),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0)
        )).isTrue();
    }

    @Test
    void parseUnknownOperatingHoursAllowsVisit() {
        OperatingHours operatingHours = parser.parse(place("12", "전화 문의", null, null, null));

        assertThat(operatingHours.isUnknown()).isTrue();
        assertThat(operatingHours.canVisit(
                LocalDate.of(2026, 9, 11),
                LocalTime.of(22, 0),
                LocalTime.of(23, 0)
        )).isTrue();
    }

    private CandidatePlace place(
            String contentTypeId,
            String useTime,
            String restDate,
            String foodOpenTime,
            String foodRestDate
    ) {
        return new CandidatePlace(
                "1",
                contentTypeId,
                "장소",
                "서울",
                null,
                "126.0",
                "37.0",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                restDate,
                useTime,
                null,
                null,
                null,
                null,
                null,
                foodOpenTime,
                foodRestDate,
                null,
                null,
                null,
                null
        );
    }
}
