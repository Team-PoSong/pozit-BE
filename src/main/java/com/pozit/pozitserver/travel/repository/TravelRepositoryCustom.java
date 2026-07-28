package com.pozit.pozitserver.travel.repository;

import com.pozit.pozitserver.travel.domain.Travel;

import java.time.LocalDate;
import java.util.List;

public interface TravelRepositoryCustom {
    List<Travel> searchPublicTravels(
            String regionCode,
            LocalDate startDate,
            LocalDate endDate,
            List<Long> tagIds,
            String keyword
    );
}
