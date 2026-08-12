package com.pozit.pozitserver.notification.scheduler;

import com.pozit.pozitserver.notification.domain.NotificationType;
import com.pozit.pozitserver.notification.service.NotificationService;
import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.travel.domain.TravelMember;
import com.pozit.pozitserver.travel.repository.TravelMemberRepository;
import com.pozit.pozitserver.travel.repository.TravelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TravelStartNotificationScheduler {

    private final TravelRepository travelRepository;
    private final TravelMemberRepository travelMemberRepository;
    private final NotificationService notificationService;

    @Transactional
    @Scheduled(cron = "0 0 9 * * *")
    public void notifyTravelsStartingTomorrow() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Travel> travels = travelRepository.findByStartDate(tomorrow);
        if (travels.isEmpty()) {
            return;
        }

        List<TravelMember> members = travelMemberRepository.findAllWithUserByTravelIn(travels);
        for (TravelMember member : members) {
            notificationService.createNotification(
                    member.getUser(),
                    member.getTravel(),
                    NotificationType.TRAVEL_START,
                    "내일 여행이 시작됩니다!"
            );
        }
    }
}
