package com.pozit.pozitserver.travel.scheduler;

import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.travel.domain.TravelStatus;
import com.pozit.pozitserver.travel.repository.TravelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TravelAutoCompleteScheduler {

    private static final ZoneId SCHEDULE_ZONE = ZoneId.of("Asia/Seoul");

    private final TravelRepository travelRepository;

    @Transactional
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void completeEndedTravels() {
        LocalDate today = LocalDate.now(SCHEDULE_ZONE);
        List<Travel> travels = travelRepository.findByStatusNotAndEndDateBefore(TravelStatus.DONE, today);
        for (Travel travel : travels) {
            travel.changeStatus(TravelStatus.DONE);
        }
    }
}
