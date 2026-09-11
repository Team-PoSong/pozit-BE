package com.pozit.pozitserver.course.scheduler;

import com.pozit.pozitserver.course.service.TouristSpotSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TouristSpotSyncScheduler {

    private final TouristSpotSyncService touristSpotSyncService;

    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
    public void syncTouristSpots() {
        touristSpotSyncService.syncAll();
    }
}
