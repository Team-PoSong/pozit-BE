package com.pozit.pozitserver.pozing.service;

import com.pozit.pozitserver.course.domain.Course;
import com.pozit.pozitserver.course.domain.CourseSpot;
import com.pozit.pozitserver.course.domain.TouristSpot;
import com.pozit.pozitserver.course.repository.CourseSpotRepository;
import com.pozit.pozitserver.pozing.domain.Pozing;
import com.pozit.pozitserver.pozing.domain.TimelapseManifest;
import com.pozit.pozitserver.pozing.model.TimelapseManifestPayload;
import com.pozit.pozitserver.pozing.repository.PozingRepository;
import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.travel.domain.TravelMember;
import com.pozit.pozitserver.travel.repository.TravelMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimelapseManifestBuilder {

    private final CourseSpotRepository courseSpotRepository;
    private final TravelMemberRepository travelMemberRepository;
    private final PozingRepository pozingRepository;

    public TimelapseManifestPayload build(Travel travel) {
        Long travelId = travel.getId();
        List<CourseSpot> courseSpots = courseSpotRepository.findAllByTravelIdForEdit(travelId);
        List<TravelMember> members = travelMemberRepository.findAllByTravelIdForEdit(travelId);
        List<Pozing> pozings = pozingRepository.findAllByTravelIdForEdit(travelId);
        Map<PozingSlot, String> pozingObjectKeysBySlot = createPozingObjectKeyMap(pozings);
        Map<Long, CourseBucket> coursesById = new LinkedHashMap<>();

        for (CourseSpot courseSpot : courseSpots) {
            Course course = courseSpot.getCourse();
            CourseBucket courseBucket = coursesById.computeIfAbsent(
                    course.getId(),
                    courseId -> new CourseBucket(course.getId(), course.getDayNumber())
            );

            courseBucket.spots().add(toSpotManifest(courseSpot, members, pozingObjectKeysBySlot));
        }

        List<TimelapseManifestPayload.CourseManifest> courses = coursesById.values()
                .stream()
                .map(courseBucket -> new TimelapseManifestPayload.CourseManifest(
                        courseBucket.courseId(),
                        courseBucket.dayNumber(),
                        courseBucket.spots()
                ))
                .toList();

        return new TimelapseManifestPayload(
                TimelapseManifest.CURRENT_VERSION,
                travelId,
                courses
        );
    }

    private Map<PozingSlot, String> createPozingObjectKeyMap(List<Pozing> pozings) {
        Map<PozingSlot, String> objectKeysBySlot = new LinkedHashMap<>();

        for (Pozing pozing : pozings) {
            objectKeysBySlot.putIfAbsent(
                    new PozingSlot(
                            pozing.getCourseSpot().getId(),
                            pozing.getUser().getId()
                    ),
                    pozing.getPozingObjectKey()
            );
        }

        return objectKeysBySlot;
    }

    private TimelapseManifestPayload.SpotManifest toSpotManifest(
            CourseSpot courseSpot,
            List<TravelMember> members,
            Map<PozingSlot, String> pozingObjectKeysBySlot
    ) {
        TouristSpot touristSpot = courseSpot.getTouristSpot();

        List<TimelapseManifestPayload.MemberPozingManifest> memberPozings = members.stream()
                .map(member -> new TimelapseManifestPayload.MemberPozingManifest(
                        member.getUser().getId(),
                        member.getUser().getNickname(),
                        pozingObjectKeysBySlot.get(new PozingSlot(
                                courseSpot.getId(),
                                member.getUser().getId()
                        ))
                ))
                .toList();

        return new TimelapseManifestPayload.SpotManifest(
                courseSpot.getId(),
                touristSpot.getId(),
                touristSpot.getName(),
                courseSpot.getOrderIndex(),
                touristSpot.getLatitude(),
                touristSpot.getLongitude(),
                memberPozings
        );
    }

    private record CourseBucket(
            Long courseId,
            Integer dayNumber,
            List<TimelapseManifestPayload.SpotManifest> spots
    ) {

        private CourseBucket(Long courseId, Integer dayNumber) {
            this(courseId, dayNumber, new ArrayList<>());
        }
    }

    private record PozingSlot(
            Long courseSpotId,
            Long userId
    ) {
    }
}
