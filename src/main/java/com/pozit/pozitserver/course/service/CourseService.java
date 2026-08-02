package com.pozit.pozitserver.course.service;

import com.pozit.pozitserver.course.constant.VisitConstants;
import com.pozit.pozitserver.course.domain.Course;
import com.pozit.pozitserver.course.domain.CourseSpot;
import com.pozit.pozitserver.course.domain.CourseSpotStatus;
import com.pozit.pozitserver.course.domain.TouristSpot;
import com.pozit.pozitserver.course.dto.request.CourseSpotUpdateRequest;
import com.pozit.pozitserver.course.dto.request.LocationRequest;
import com.pozit.pozitserver.course.dto.response.CourseDetailResponse;
import com.pozit.pozitserver.course.dto.response.CurrentLocationResponse;
import com.pozit.pozitserver.course.dto.response.NearbySpotResponse;
import com.pozit.pozitserver.course.repository.CourseRepository;
import com.pozit.pozitserver.course.repository.CourseSpotRepository;
import com.pozit.pozitserver.course.repository.TouristSpotRepository;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.global.s3.S3Service;
import com.pozit.pozitserver.global.util.GeoUtils;
import com.pozit.pozitserver.pozing.domain.Pozing;
import com.pozit.pozitserver.pozing.repository.PozingRepository;
import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.travel.domain.TravelMember;
import com.pozit.pozitserver.travel.domain.TravelMemberRole;
import com.pozit.pozitserver.travel.domain.TravelStatus;
import com.pozit.pozitserver.travel.repository.TravelMemberRepository;
import com.pozit.pozitserver.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private static final java.time.Duration POZING_GET_URL_EXPIRATION = java.time.Duration.ofMinutes(10);

    private final CourseRepository courseRepository;
    private final CourseSpotRepository courseSpotRepository;
    private final PozingRepository pozingRepository;
    private final TouristSpotRepository touristSpotRepository;
    private final TravelMemberRepository travelMemberRepository;
    private final S3Service s3Service;

    /**
     * 코스 상세 조회 (해당 여행 멤버만 가능)
     */
    public CourseDetailResponse getCourseDetail(User currentUser, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON404));

        validateMember(course.getTravel(), currentUser);

        return buildCourseDetailResponse(course);
    }

    /**
     * 공개 여행의 코스 상세 조회 (완료 + 공개 여행인 경우만, 비로그인 접근 가능)
     */
    public CourseDetailResponse getPublicCourseDetail(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON404));

        if (!course.getTravel().isPubliclyVisible()) {
            throw new BusinessException(ErrorCode.COMMON404);
        }

        return buildCourseDetailResponse(course);
    }

    private CourseDetailResponse buildCourseDetailResponse(Course course) {
        List<CourseSpot> spots = courseSpotRepository.findAllByCourseInOrder(List.of(course));

        List<Pozing> pozings = spots.isEmpty()
                ? List.of()
                : pozingRepository.findAllWithUserByCourseSpotIn(spots);

        Map<Long, List<Pozing>> pozingsBySpotId = pozings.stream()
                .collect(Collectors.groupingBy(p -> p.getCourseSpot().getId()));

        List<CourseDetailResponse.CourseSpotDetail> spotDetails = spots.stream()
                .map(spot -> toSpotDetail(spot, pozingsBySpotId.getOrDefault(spot.getId(), List.of())))
                .toList();

        Long initialFocusSpotId = calculateInitialFocusSpotId(spots);

        return new CourseDetailResponse(course.getId(), course.getDayNumber(), course.getDate(), initialFocusSpotId, spotDetails);
    }

    /**
     * 처음 진입 시 포커스할 spot을 계산
     * VISITED 중 orderIndex가 가장 큰 spot의 다음 spot을 지정하고,
     * VISITED가 없으면 첫 spot을, 전부 VISITED면 마지막 spot을 지정.
     */
    private Long calculateInitialFocusSpotId(List<CourseSpot> spots) {
        if (spots.isEmpty()) {
            return null;
        }

        int lastVisitedIndex = -1;
        for (int i = 0; i < spots.size(); i++) {
            if (spots.get(i).getStatus() == CourseSpotStatus.VISITED) {
                lastVisitedIndex = i;
            }
        }

        int focusIndex = (lastVisitedIndex == -1) ? 0 : Math.min(lastVisitedIndex + 1, spots.size() - 1);
        return spots.get(focusIndex).getId();
    }

    private CourseDetailResponse.CourseSpotDetail toSpotDetail(CourseSpot spot, List<Pozing> pozings) {
        TouristSpot touristSpot = spot.getTouristSpot();

        List<CourseDetailResponse.PozingInfo> pozingInfos = pozings.stream()
                .map(p -> new CourseDetailResponse.PozingInfo(
                        p.getId(),
                        s3Service.createGetPresignedUrl(p.getPozingObjectKey(), POZING_GET_URL_EXPIRATION),
                        p.getUser().getNickname()
                ))
                .toList();

        return new CourseDetailResponse.CourseSpotDetail(
                spot.getId(),
                touristSpot.getId(),
                touristSpot.getName(),
                touristSpot.getAddress(),
                touristSpot.getLatitude() != null ? touristSpot.getLatitude().doubleValue() : null,
                touristSpot.getLongitude() != null ? touristSpot.getLongitude().doubleValue() : null,
                touristSpot.getImageUrl(),
                spot.getOrderIndex(),
                spot.getStatus().name(),
                pozingInfos
        );
    }

    /**
     * 내 위치와 기준 반경 100m 이내 장소 조회 (해당 여행 멤버만 가능)
     */
    public CurrentLocationResponse getNearbySpots(User currentUser, Long courseId, LocationRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON404));

        validateMember(course.getTravel(), currentUser);

        List<CourseSpot> spots = courseSpotRepository.findAllByCourseInOrder(List.of(course));

        List<NearbySpotResponse> nearbySpots = spots.stream()
                .filter(spot -> spot.getStatus() == CourseSpotStatus.NOT_VISITED)
                .filter(spot -> spot.getTouristSpot().getLatitude() != null && spot.getTouristSpot().getLongitude() != null)
                .map(spot -> toNearbySpotResponse(spot, request))
                .filter(response -> response.distanceMeters() <= VisitConstants.VISITING_RADIUS_METERS)
                .sorted(Comparator.comparingDouble(NearbySpotResponse::distanceMeters))
                .toList();

        return new CurrentLocationResponse(request.latitude(), request.longitude(), nearbySpots);
    }

    private NearbySpotResponse toNearbySpotResponse(CourseSpot spot, LocationRequest request) {
        TouristSpot touristSpot = spot.getTouristSpot();

        double distanceMeters = GeoUtils.calculateDistance(
                request.latitude(),
                request.longitude(),
                touristSpot.getLatitude().doubleValue(),
                touristSpot.getLongitude().doubleValue()
        );

        return new NearbySpotResponse(spot.getId(), touristSpot.getId(), touristSpot.getName(), distanceMeters);
    }

    /**
     * 코스 장소 목록 수정 (리더만 가능, DONE 상태 여행은 수정 불가)
     * diff 방식: 유지되는 장소는 순서만 갱신, 삭제되는 장소는 포징도 함께 삭제), 새로 추가된 장소는 신규 생성
     */
    @Transactional
    public void updateCourseSpots(User currentUser, Long courseId, CourseSpotUpdateRequest request) {
        Course course = courseRepository.findByIdForUpdate(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON404));

        Travel travel = course.getTravel();
        validateLeader(travel, currentUser);

        if (travel.getStatus() == TravelStatus.DONE) {
            throw new BusinessException(ErrorCode.COMPLETED_TRAVEL_COURSE_NOT_EDITABLE);
        }

        List<Long> touristSpotIds = request.touristSpotIds();
        Set<Long> newTouristSpotIds = new HashSet<>(touristSpotIds);

        if (touristSpotIds.size() != newTouristSpotIds.size()) {
            throw new BusinessException(ErrorCode.DUPLICATE_COURSE_SPOT);
        }

        List<CourseSpot> existingSpots = courseSpotRepository.findAllByCourseInOrder(List.of(course));
        Map<Long, CourseSpot> existingByTouristSpotId = existingSpots.stream()
                .collect(Collectors.toMap(spot -> spot.getTouristSpot().getId(), spot -> spot));

        List<CourseSpot> spotsToUpdate = new ArrayList<>();
        List<Integer> targetOrderIndexes = new ArrayList<>();
        List<Long> touristSpotIdsToCreate = new ArrayList<>();
        List<Integer> orderIndexesToCreate = new ArrayList<>();

        for (int i = 0; i < touristSpotIds.size(); i++) {
            Long touristSpotId = touristSpotIds.get(i);
            int orderIndex = i + 1;
            CourseSpot existingSpot = existingByTouristSpotId.get(touristSpotId);

            if (existingSpot != null) {
                spotsToUpdate.add(existingSpot);
                targetOrderIndexes.add(orderIndex);
            } else {
                touristSpotIdsToCreate.add(touristSpotId);
                orderIndexesToCreate.add(orderIndex);
            }
        }

        Map<Long, TouristSpot> touristSpotsToCreateById = Map.of();
        if (!touristSpotIdsToCreate.isEmpty()) {
            List<TouristSpot> foundTouristSpots = touristSpotRepository.findAllById(touristSpotIdsToCreate);
            if (foundTouristSpots.size() != new HashSet<>(touristSpotIdsToCreate).size()) {
                throw new BusinessException(ErrorCode.COMMON404);
            }
            touristSpotsToCreateById = foundTouristSpots.stream()
                    .collect(Collectors.toMap(TouristSpot::getId, ts -> ts));
        }

        List<CourseSpot> spotsToRemove = existingSpots.stream()
                .filter(spot -> !newTouristSpotIds.contains(spot.getTouristSpot().getId()))
                .toList();

        if (!spotsToRemove.isEmpty()) {
            pozingRepository.deleteAllInBatch(pozingRepository.findByCourseSpotIn(spotsToRemove));
            courseSpotRepository.deleteAllInBatch(spotsToRemove);
        }

        // 임시값으로 먼저 옮긴 뒤 최종값을 확정
        int tempOffset = existingSpots.size() + touristSpotIds.size();
        for (int i = 0; i < spotsToUpdate.size(); i++) {
            spotsToUpdate.get(i).updateOrderIndex(tempOffset + i + 1);
        }
        courseSpotRepository.flush();

        for (int i = 0; i < spotsToUpdate.size(); i++) {
            spotsToUpdate.get(i).updateOrderIndex(targetOrderIndexes.get(i));
        }

        List<CourseSpot> spotsToCreate = new ArrayList<>();
        for (int i = 0; i < touristSpotIdsToCreate.size(); i++) {
            TouristSpot touristSpot = touristSpotsToCreateById.get(touristSpotIdsToCreate.get(i));
            spotsToCreate.add(CourseSpot.builder()
                    .course(course)
                    .touristSpot(touristSpot)
                    .orderIndex(orderIndexesToCreate.get(i))
                    .build());
        }

        if (!spotsToCreate.isEmpty()) {
            courseSpotRepository.saveAll(spotsToCreate);
        }
    }

    private void validateMember(Travel travel, User user) {
        boolean isMember = travelMemberRepository.existsByTravelAndUser(travel, user);
        if (!isMember) {
            throw new BusinessException(ErrorCode.COMMON403);
        }
    }

    private void validateLeader(Travel travel, User user) {
        TravelMember member = travelMemberRepository.findByTravelAndUser(travel, user)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON403));

        if (member.getRole() != TravelMemberRole.LEADER) {
            throw new BusinessException(ErrorCode.COMMON403);
        }
    }
}
