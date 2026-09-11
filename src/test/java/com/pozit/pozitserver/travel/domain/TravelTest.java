package com.pozit.pozitserver.travel.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TravelTest {

    @Test
    void calculateStatusReturnsInProgressWhenTodayIsStartDate() {
        Travel travel = travel(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12));

        TravelStatus status = travel.calculateStatus(LocalDate.of(2026, 9, 10));

        assertThat(status).isEqualTo(TravelStatus.IN_PROGRESS);
    }

    @Test
    void calculateStatusReturnsInProgressWhenTodayIsEndDate() {
        Travel travel = travel(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 10));

        TravelStatus status = travel.calculateStatus(LocalDate.of(2026, 9, 10));

        assertThat(status).isEqualTo(TravelStatus.IN_PROGRESS);
    }

    @Test
    void calculateStatusReturnsInProgressWhenTodayIsBetweenStartAndEndDate() {
        Travel travel = travel(LocalDate.of(2026, 9, 9), LocalDate.of(2026, 9, 11));

        TravelStatus status = travel.calculateStatus(LocalDate.of(2026, 9, 10));

        assertThat(status).isEqualTo(TravelStatus.IN_PROGRESS);
    }

    @Test
    void calculateStatusReturnsBeforeWhenTodayIsBeforeStartDate() {
        Travel travel = travel(LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 12));

        TravelStatus status = travel.calculateStatus(LocalDate.of(2026, 9, 10));

        assertThat(status).isEqualTo(TravelStatus.BEFORE);
    }

    @Test
    void calculateStatusReturnsDoneWhenTodayIsAfterEndDate() {
        Travel travel = travel(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 9));

        TravelStatus status = travel.calculateStatus(LocalDate.of(2026, 9, 10));

        assertThat(status).isEqualTo(TravelStatus.DONE);
    }

    @Test
    void calculateStatusKeepsPersistedDoneStatus() {
        Travel travel = travel(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12));
        travel.changeStatus(TravelStatus.DONE);

        TravelStatus status = travel.calculateStatus(LocalDate.of(2026, 9, 10));

        assertThat(status).isEqualTo(TravelStatus.DONE);
    }

    @Test
    void calculateStatusKeepsDraftStatus() {
        Travel travel = travel(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12));
        travel.markDraft();

        TravelStatus status = travel.calculateStatus(LocalDate.of(2026, 9, 10));

        assertThat(status).isEqualTo(TravelStatus.DRAFT);
    }

    @Test
    void confirmDraftChangesStatusByTravelPeriod() {
        Travel travel = travel(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12));
        travel.markDraft();

        travel.confirmDraft(LocalDate.of(2026, 9, 10));

        assertThat(travel.getStatus()).isEqualTo(TravelStatus.IN_PROGRESS);
    }

    private Travel travel(LocalDate startDate, LocalDate endDate) {
        return Travel.builder()
                .title("서울 여행")
                .destination("서울")
                .regionCode("11")
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}
