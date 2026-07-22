package com.pozit.pozitserver.travel.service;

import com.pozit.pozitserver.course.domain.Course;
import com.pozit.pozitserver.course.domain.CourseSpot;
import com.pozit.pozitserver.course.domain.CourseSpotStatus;
import com.pozit.pozitserver.course.repository.CourseRepository;
import com.pozit.pozitserver.course.repository.CourseSpotRepository;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.pozing.domain.Pozing;
import com.pozit.pozitserver.pozing.repository.PozingRepository;
import com.pozit.pozitserver.tag.domain.TravelTag;
import com.pozit.pozitserver.tag.repository.TravelTagRepository;
import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.travel.domain.TravelMember;
import com.pozit.pozitserver.travel.domain.TravelMemberRole;
import com.pozit.pozitserver.travel.domain.TravelStatus;
import com.pozit.pozitserver.travel.dto.response.TravelDetailResponse;
import com.pozit.pozitserver.travel.dto.response.TravelListResponse;
import com.pozit.pozitserver.travel.repository.TravelMemberRepository;
import com.pozit.pozitserver.travel.repository.TravelRepository;
import com.pozit.pozitserver.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelService {

    private final TravelRepository travelRepository;
    private final TravelMemberRepository travelMemberRepository;
    private final TravelTagRepository travelTagRepository;
    private final CourseRepository courseRepository;
    private final CourseSpotRepository courseSpotRepository;
    private final PozingRepository pozingRepository;

    /**
     * 여행 목록 조회
     */
    public List<TravelListResponse> getTravels(User currentUser, boolean isDone) {
        List<TravelMember> myMemberships = travelMemberRepository.findAllWithTravelByUser(currentUser);

        List<Travel> travels = myMemberships.stream()
                .map(TravelMember::getTravel)
                .filter(travel -> isDone
                        ? travel.getStatus() == TravelStatus.DONE
                        : travel.getStatus() != TravelStatus.DONE)
                .sorted(Comparator.comparing(Travel::getStartDate))
                .toList();

        if (travels.isEmpty()) {
            return List.of();
        }

        Map<Long, List<TravelMember>> membersByTravelId = travelMemberRepository
                .findAllWithUserByTravelIn(travels).stream()
                .collect(Collectors.groupingBy(m -> m.getTravel().getId()));

        Map<Long, List<TravelTag>> tagsByTravelId = travelTagRepository
                .findAllWithTagByTravelIn(travels).stream()
                .collect(Collectors.groupingBy(t -> t.getTravel().getId()));

        List<Course> allCourses = courseRepository.findByTravelInOrderByDayNumberAsc(travels);
        Map<Long, List<CourseSpot>> spotsByTravelId = getSpotsGroupedByTravelId(allCourses);

        return travels.stream()
                .map(travel -> toTravelListResponse(
                        travel,
                        membersByTravelId.getOrDefault(travel.getId(), List.of()),
                        tagsByTravelId.getOrDefault(travel.getId(), List.of()),
                        spotsByTravelId.getOrDefault(travel.getId(), List.of())
                ))
                .toList();
    }

    private TravelListResponse toTravelListResponse(
            Travel travel,
            List<TravelMember> members,
            List<TravelTag> travelTags,
            List<CourseSpot> spots
    ) {
        List<String> tags = travelTags.stream().map(t -> t.getTag().getName()).toList();
        int completionRate = calculateCompletionRate(spots);

        String leaderNickname = members.stream()
                .filter(m -> m.getRole() == TravelMemberRole.LEADER)
                .findFirst()
                .map(m -> m.getUser().getNickname())
                .orElse(null);

        return new TravelListResponse(
                travel.getId(),
                travel.getTitle(),
                travel.getDestination(),
                travel.getStartDate(),
                travel.getEndDate(),
                travel.getStatus().name(),
                travel.getIsPublic(),
                travel.getBackgroundImageUrl(),
                completionRate,
                tags,
                leaderNickname,
                members.size()
        );
    }

    /**
     * 여행 상세 조회
     */
    public TravelDetailResponse getTravelDetail(User currentUser, Long travelId) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON404));

        validateMember(travel, currentUser);

        List<TravelMember> members = travelMemberRepository.findAllWithUserByTravelIn(List.of(travel));
        List<String> tags = travelTagRepository.findAllWithTagByTravelIn(List.of(travel)).stream()
                .map(travelTag -> travelTag.getTag().getName())
                .toList();
        List<Course> courses = courseRepository.findByTravelOrderByDayNumberAsc(travel);

        List<CourseSpot> allSpots = courses.isEmpty()
                ? List.of()
                : courseSpotRepository.findAllByCourseInOrder(courses);

        Map<Long, List<CourseSpot>> spotsByCourseId = new HashMap<>();
        for (CourseSpot spot : allSpots) {
            spotsByCourseId
                    .computeIfAbsent(spot.getCourse().getId(), k -> new ArrayList<>())
                    .add(spot);
        }

        List<Pozing> allPozings = allSpots.isEmpty()
                ? List.of()
                : pozingRepository.findAllWithUserByCourseSpotIn(allSpots);

        Map<Long, List<Pozing>> pozingsByCourseSpotId = allPozings.stream()
                .collect(Collectors.groupingBy(p -> p.getCourseSpot().getId()));

        int totalSpotCount = allSpots.size();
        int totalPozingCount = allPozings.size();
        int completionRate = calculateCompletionRate(allSpots);

        List<TravelDetailResponse.CourseInfo> courseInfos = courses.stream()
                .map(course -> toCourseInfo(course, spotsByCourseId, pozingsByCourseSpotId))
                .toList();

        List<TravelDetailResponse.MemberInfo> memberInfos = members.stream()
                .map(m -> new TravelDetailResponse.MemberInfo(
                        m.getUser().getId(),
                        m.getUser().getNickname(),
                        m.getRole().name()
                ))
                .toList();

        return new TravelDetailResponse(
                travel.getId(),
                travel.getTitle(),
                travel.getDestination(),
                travel.getStartDate(),
                travel.getEndDate(),
                travel.getStatus().name(),
                travel.getIsPublic(),
                travel.getBackgroundImageUrl(),
                travel.getInviteCode(),
                completionRate,
                totalSpotCount,
                totalPozingCount,
                tags,
                memberInfos,
                courseInfos
        );
    }

    private TravelDetailResponse.CourseInfo toCourseInfo(
            Course course,
            Map<Long, List<CourseSpot>> spotsByCourseId,
            Map<Long, List<Pozing>> pozingsByCourseSpotId
    ) {
        List<CourseSpot> spots = spotsByCourseId.getOrDefault(course.getId(), List.of());

        List<TravelDetailResponse.CourseSpotInfo> spotInfos = spots.stream()
                .map(spot -> {
                    List<Pozing> pozings = pozingsByCourseSpotId.getOrDefault(spot.getId(), List.of());
                    List<TravelDetailResponse.PozingInfo> pozingInfos = pozings.stream()
                            .map(p -> new TravelDetailResponse.PozingInfo(
                                    p.getId(),
                                    p.getUser().getId(),
                                    p.getUser().getNickname(),
                                    p.getPozingUrl(),
                                    p.getThumbnailUrl()
                            ))
                            .toList();

                    return new TravelDetailResponse.CourseSpotInfo(
                            spot.getId(),
                            spot.getTouristSpot().getId(),
                            spot.getTouristSpot().getName(),
                            spot.getTouristSpot().getLatitude(),
                            spot.getTouristSpot().getLongitude(),
                            spot.getOrderIndex(),
                            spot.getStatus().name(),
                            pozingInfos
                    );
                })
                .toList();

        return new TravelDetailResponse.CourseInfo(
                course.getId(),
                course.getDayNumber(),
                course.getDate(),
                spotInfos
        );
    }

    /**
     * 목록 조회용: 여러 여행의 코스를 travelId 기준으로 spot까지 그룹핑
     */
    private Map<Long, List<CourseSpot>> getSpotsGroupedByTravelId(List<Course> allCourses) {
        if (allCourses.isEmpty()) {
            return Map.of();
        }

        List<CourseSpot> allSpots = courseSpotRepository.findAllByCourseInOrder(allCourses);

        Map<Long, List<CourseSpot>> result = new HashMap<>();
        for (CourseSpot spot : allSpots) {
            Long travelId = spot.getCourse().getTravel().getId();
            result.computeIfAbsent(travelId, k -> new ArrayList<>()).add(spot);
        }
        return result;
    }

    private int calculateCompletionRate(List<CourseSpot> spots) {
        if (spots.isEmpty()) {
            return 0;
        }
        long visited = spots.stream()
                .filter(s -> s.getStatus() == CourseSpotStatus.VISITED)
                .count();
        return (int) Math.round((visited * 100.0) / spots.size());
    }

    private void validateMember(Travel travel, User user) {
        boolean isMember = travelMemberRepository.existsByTravelAndUser(travel, user);
        if (!isMember) {
            throw new BusinessException(ErrorCode.COMMON403);
        }
    }
}
