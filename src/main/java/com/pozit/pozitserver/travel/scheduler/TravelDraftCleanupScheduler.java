package com.pozit.pozitserver.travel.scheduler;

import com.pozit.pozitserver.travel.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class TravelDraftCleanupScheduler {

    private static final Duration DRAFT_EXPIRATION = Duration.ofHours(24);

    private final TravelService travelService;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void deleteExpiredDraftTravels() {
        travelService.deleteExpiredDraftTravels(DRAFT_EXPIRATION);
    }
}
