package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.course.domain.Course;
import com.pozit.pozitserver.course.domain.CourseSpot;
import com.pozit.pozitserver.course.domain.TouristSpot;
import com.pozit.pozitserver.course.repository.CourseRepository;
import com.pozit.pozitserver.course.repository.CourseSpotRepository;
import com.pozit.pozitserver.course.repository.TouristSpotRepository;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.recommendation.dto.RecommendedCourseCardResponse;
import com.pozit.pozitserver.recommendation.dto.RecommendedCourseResponse;
import com.pozit.pozitserver.recommendation.dto.RecommendedCourseSaveRequest;
import com.pozit.pozitserver.recommendation.model.CandidatePlace;
import com.pozit.pozitserver.recommendation.model.CourseRecommendCommand;
import com.pozit.pozitserver.recommendation.model.PlaceFeatureVector;
import com.pozit.pozitserver.recommendation.model.RecommendationTag;
import com.pozit.pozitserver.recommendation.model.ScoredPlace;
import com.pozit.pozitserver.pozing.repository.PozingRepository;
import com.pozit.pozitserver.tag.repository.TravelTagRepository;
import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.travel.domain.TravelMember;
import com.pozit.pozitserver.travel.domain.TravelMemberRole;
import com.pozit.pozitserver.travel.domain.TravelStatus;
import com.pozit.pozitserver.travel.dto.response.PublicTravelListResponse;
import com.pozit.pozitserver.travel.repository.TravelMemberRepository;
import com.pozit.pozitserver.travel.repository.TravelRepository;
import com.pozit.pozitserver.travel.service.TravelService;
import com.pozit.pozitserver.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CourseRecommendationService {

    private final TravelRepository travelRepository;
    private final TravelMemberRepository travelMemberRepository;
    private final TravelTagRepository travelTagRepository;
    private final CourseRepository courseRepository;
    private final CourseSpotRepository courseSpotRepository;
    private final TouristSpotRepository touristSpotRepository;
    private final PozingRepository pozingRepository;
    private final TourApiCandidateProvider candidateProvider;
    private final UserPreferenceVectorFactory userPreferenceVectorFactory;
    private final PlaceFeatureExtractor placeFeatureExtractor;
    private final ContentScoreCalculator contentScoreCalculator;
    private final ContextualRankingService contextualRankingService;
    private final DiversityRerankingService diversityRerankingService;
    private final RouteOptimizationService routeOptimizationService;
    private final TravelService travelService;
    private final RecommendationPreviewStore recommendationPreviewStore;

    public RecommendedCourseResponse preview(Long travelId, User currentUser) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAVEL_NOT_FOUND));
        validateMember(travel, currentUser);

        return createPreview(travel);
    }

    public RecommendedCourseCardResponse previewCard(Long travelId, User currentUser) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAVEL_NOT_FOUND));
        validateMember(travel, currentUser);

        RecommendedCourseResponse recommendedCourse = createPreview(travel);
        String previewId = recommendationPreviewStore.save(travel.getId(), currentUser.getId(), recommendedCourse);
        List<String> tags = travelTagRepository.findTagNamesByTravelId(travel.getId());
        if (tags.isEmpty()) {
            tags = toCommand(travel).tags().stream()
                    .map(RecommendationTag::koreanName)
                    .toList();
        }

        List<RecommendedCourseResponse.RecommendedPlaceResponse> previewPlaces = recommendedCourse.days().stream()
                .flatMap(day -> day.places().stream())
                .toList();

        List<String> imageUrls = previewPlaces.stream()
                .map(RecommendedCourseResponse.RecommendedPlaceResponse::imageUrl)
                .filter(imageUrl -> imageUrl != null && !imageUrl.isBlank())
                .distinct()
                .limit(3)
                .toList();
        if (imageUrls.isEmpty() && travel.getBackgroundImageUrl() != null && !travel.getBackgroundImageUrl().isBlank()) {
            imageUrls = List.of(travel.getBackgroundImageUrl());
        }

        return new RecommendedCourseCardResponse(
                previewId,
                RecommendationPreviewStore.TTL_SECONDS,
                travel.getId(),
                "Pozit Pick!",
                createCardTitle(travel),
                travel.getTitle(),
                travel.getDestination(),
                travel.getStartDate(),
                travel.getEndDate(),
                recommendedCourse.dayCount(),
                Math.max(recommendedCourse.dayCount() - 1, 0),
                createPeriodText(travel.getStartDate(), travel.getEndDate()),
                imageUrls.isEmpty() ? null : imageUrls.get(0),
                imageUrls,
                tags,
                Math.toIntExact(travelMemberRepository.countByTravel(travel)),
                previewPlaces.size(),
                findRelatedPublicTravels(travel, currentUser)
        );
    }

    public RecommendedCourseResponse getPreview(Long travelId, User currentUser, String previewId) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAVEL_NOT_FOUND));
        validateMember(travel, currentUser);

        return recommendationPreviewStore.find(previewId, travel.getId(), currentUser.getId());
    }

    private RecommendedCourseResponse createPreview(Travel travel) {
        CourseRecommendCommand command = toCommand(travel);
        List<CandidatePlace> candidates = candidateProvider.findCandidates(command);
        if (candidates.isEmpty()) {
            candidates = findStoredCandidateFallback(travel);
        }
        if (candidates.isEmpty()) {
            log.warn("No recommendable candidates found from Tour API or stored spots. travelId={}, destination={}, regionCode={}, tags={}",
                    travel.getId(),
                    travel.getDestination(),
                    travel.getRegionCode(),
                    command.tags()
            );
            throw new BusinessException(ErrorCode.RECOMMENDABLE_PLACE_NOT_FOUND);
        }

        double[] userVector = userPreferenceVectorFactory.create(command.tags());

        List<ScoredPlace> scoredPlaces = candidates.stream()
                .map(place -> scorePlace(place, userVector, command))
                .toList();

        List<ScoredPlace> rankedPlaces = contextualRankingService.rank(scoredPlaces, command);
        List<ScoredPlace> diversifiedPlaces = diversityRerankingService.rerank(rankedPlaces);

        RecommendedCourseResponse recommendedCourse = routeOptimizationService.createCourse(diversifiedPlaces, command);
        if (hasNoSaveablePlaces(recommendedCourse)) {
            log.info("Recommendation result has no saveable places. travelId={}, destination={}, regionCode={}, candidates={}",
                    travel.getId(),
                    travel.getDestination(),
                    travel.getRegionCode(),
                    candidates.size()
            );
            throw new BusinessException(ErrorCode.RECOMMENDABLE_PLACE_NOT_FOUND);
        }

        return recommendedCourse;
    }

    private List<CandidatePlace> findStoredCandidateFallback(Travel travel) {
        String legalDongRegionCode = legalDongRegionCode(travel.getRegionCode());
        String legalDongSigunguCode = legalDongSigunguCode(travel.getRegionCode());

        List<TouristSpot> touristSpots = touristSpotRepository.findRecommendableByRegion(
                travel.getRegionCode(),
                legalDongRegionCode,
                legalDongSigunguCode,
                PageRequest.of(0, 40)
        );
        if (touristSpots.isEmpty()) {
            touristSpots = touristSpotRepository.findRecommendable(PageRequest.of(0, 40));
        }

        if (!touristSpots.isEmpty()) {
            log.info("Using stored tourist spot fallback for recommendation. travelId={}, regionCode={}, candidateCount={}",
                    travel.getId(),
                    travel.getRegionCode(),
                    touristSpots.size()
            );
        }

        return touristSpots.stream()
                .map(this::toCandidatePlace)
                .toList();
    }

    private CandidatePlace toCandidatePlace(TouristSpot touristSpot) {
        return new CandidatePlace(
                touristSpot.getContentId(),
                touristSpot.getContentTypeId(),
                touristSpot.getName(),
                touristSpot.getAddress(),
                touristSpot.getImageUrl(),
                touristSpot.getLongitude().toPlainString(),
                touristSpot.getLatitude().toPlainString(),
                null,
                null,
                null,
                touristSpot.getLegalDongRegionCode(),
                touristSpot.getLegalDongSigunguCode(),
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
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private String legalDongRegionCode(String regionCode) {
        if (regionCode == null || regionCode.isBlank()) {
            return "";
        }
        String normalizedCode = regionCode.trim();
        return normalizedCode.length() >= 2 ? normalizedCode.substring(0, 2) : "";
    }

    private String legalDongSigunguCode(String regionCode) {
        if (regionCode == null || regionCode.isBlank()) {
            return "";
        }
        String normalizedCode = regionCode.trim();
        return normalizedCode.length() >= 5 && !normalizedCode.endsWith("000") ? normalizedCode : "";
    }

    private boolean hasNoSaveablePlaces(RecommendedCourseResponse recommendedCourse) {
        return recommendedCourse.days().stream()
                .flatMap(day -> day.places().stream())
                .noneMatch(place -> place.contentId() != null
                        && !place.contentId().isBlank()
                        && place.title() != null
                        && !place.title().isBlank());
    }

    private String createCardTitle(Travel travel) {
        return travel.getStartDate().getMonthValue() + "월 추천, "
                + travel.getDestination() + topicParticle(travel.getDestination()) + " 어때요?";
    }

    private String createPeriodText(LocalDate startDate, LocalDate endDate) {
        int dayCount = (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
        int nightCount = Math.max(dayCount - 1, 0);
        return startDate.getMonthValue() + "/" + startDate.getDayOfMonth()
                + " - "
                + endDate.getMonthValue() + "/" + endDate.getDayOfMonth()
                + " · "
                + nightCount + "박 " + dayCount + "일";
    }

    private String topicParticle(String value) {
        if (value == null || value.isBlank()) {
            return "은";
        }

        char lastChar = value.trim().charAt(value.trim().length() - 1);
        if (lastChar < '가' || lastChar > '힣') {
            return "은";
        }

        return (lastChar - '가') % 28 == 0 ? "는" : "은";
    }

    private List<PublicTravelListResponse> findRelatedPublicTravels(Travel travel, User currentUser) {
        List<PublicTravelListResponse> publicTravels = travelService.getPublicTravels(
                currentUser,
                travel.getRegionCode(),
                null,
                null,
                null,
                null
        );

        if (publicTravels.isEmpty()) {
            return List.of();
        }

        boolean hasLike = publicTravels.stream()
                .anyMatch(publicTravel -> publicTravel.likeCount() != null && publicTravel.likeCount() > 0);
        if (!hasLike) {
            List<PublicTravelListResponse> shuffledTravels = new ArrayList<>(publicTravels);
            Collections.shuffle(shuffledTravels);
            return shuffledTravels.stream()
                    .limit(2)
                    .toList();
        }

        return publicTravels.stream()
                .sorted(Comparator.comparing(
                                (PublicTravelListResponse publicTravel) -> publicTravel.likeCount() == null
                                        ? 0
                                        : publicTravel.likeCount()
                        )
                        .reversed()
                        .thenComparing(PublicTravelListResponse::endDate, Comparator.reverseOrder())
                        .thenComparing(PublicTravelListResponse::travelId, Comparator.reverseOrder()))
                .limit(2)
                .toList();
    }

    @Transactional
    public void commit(Long travelId, User currentUser, RecommendedCourseSaveRequest request) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAVEL_NOT_FOUND));
        validateLeader(travel, currentUser);

        if (travel.getStatus() == TravelStatus.DONE) {
            throw new BusinessException(ErrorCode.COMPLETED_TRAVEL_COURSE_NOT_EDITABLE);
        }

        Map<Integer, Course> coursesByDayNumber = courseRepository.findByTravelOrderByDayNumberAsc(travel).stream()
                .collect(Collectors.toMap(Course::getDayNumber, Function.identity()));

        validateRequestedDays(request, coursesByDayNumber);

        List<RecommendedCourseSaveRequest.RecommendedPlaceSaveRequest> requestedPlaces = request.days().stream()
                .flatMap(day -> day.places().stream())
                .toList();
        Map<String, TouristSpot> touristSpotsByContentId = saveMissingTouristSpots(requestedPlaces);

        List<Course> targetCourses = request.days().stream()
                .map(day -> coursesByDayNumber.get(day.dayNumber()))
                .toList();
        List<CourseSpot> existingSpots = courseSpotRepository.findAllByCourseInOrder(targetCourses);
        if (!existingSpots.isEmpty()) {
            pozingRepository.deleteAllInBatch(pozingRepository.findByCourseSpotIn(existingSpots));
            courseSpotRepository.deleteAllInBatch(existingSpots);
            courseSpotRepository.flush();
        }

        List<CourseSpot> courseSpots = new ArrayList<>();
        for (RecommendedCourseSaveRequest.RecommendedDaySaveRequest day : request.days()) {
            Course course = coursesByDayNumber.get(day.dayNumber());

            day.places().stream()
                    .sorted(Comparator.comparingInt(RecommendedCourseSaveRequest.RecommendedPlaceSaveRequest::orderIndex))
                    .forEach(place -> courseSpots.add(CourseSpot.builder()
                            .course(course)
                            .touristSpot(touristSpotsByContentId.get(place.contentId()))
                            .orderIndex(place.orderIndex())
                            .build()));
        }

        courseSpotRepository.saveAll(courseSpots);
    }

    private ScoredPlace scorePlace(CandidatePlace place, double[] userVector, CourseRecommendCommand command) {
        PlaceFeatureVector featureVector = placeFeatureExtractor.extract(place);
        double contentScore = contentScoreCalculator.calculate(userVector, featureVector, command.tags());

        return ScoredPlace.contentOnly(place, featureVector, contentScore);
    }

    private CourseRecommendCommand toCommand(Travel travel) {
        List<RecommendationTag> tags = travelTagRepository.findTagNamesByTravelId(travel.getId()).stream()
                .map(RecommendationTag::fromName)
                .flatMap(Optional::stream)
                .distinct()
                .limit(2)
                .toList();

        if (tags.isEmpty()) {
            tags = List.of(RecommendationTag.RECORD, RecommendationTag.CULTURE);
        }

        return new CourseRecommendCommand(
                travel.getId(),
                travel.getDestination(),
                travel.getRegionCode(),
                travel.getStartDate(),
                travel.getEndDate(),
                travel.getTravelStyle(),
                travel.getTransportation(),
                tags
        );
    }

    private void validateMember(Travel travel, User user) {
        if (!travelMemberRepository.existsByTravelAndUser(travel, user)) {
            throw new BusinessException(ErrorCode.NOT_VALID_TRAVEL_MEMBER);
        }
    }

    private void validateLeader(Travel travel, User user) {
        TravelMember member = travelMemberRepository.findByTravelAndUser(travel, user)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON403));

        if (member.getRole() != TravelMemberRole.LEADER) {
            throw new BusinessException(ErrorCode.COMMON403);
        }
    }

    private void validateRequestedDays(
            RecommendedCourseSaveRequest request,
            Map<Integer, Course> coursesByDayNumber
    ) {
        Set<Integer> dayNumbers = new HashSet<>();

        for (RecommendedCourseSaveRequest.RecommendedDaySaveRequest day : request.days()) {
            if (!coursesByDayNumber.containsKey(day.dayNumber()) || !dayNumbers.add(day.dayNumber())) {
                throw new BusinessException(ErrorCode.COMMON400);
            }

            Set<String> contentIds = new HashSet<>();
            Set<Integer> orderIndexes = new HashSet<>();
            for (RecommendedCourseSaveRequest.RecommendedPlaceSaveRequest place : day.places()) {
                if (!contentIds.add(place.contentId()) || !orderIndexes.add(place.orderIndex())) {
                    throw new BusinessException(ErrorCode.DUPLICATE_COURSE_SPOT);
                }
            }
        }
    }

    private Map<String, TouristSpot> saveMissingTouristSpots(
            List<RecommendedCourseSaveRequest.RecommendedPlaceSaveRequest> requestedPlaces
    ) {
        Map<String, RecommendedCourseSaveRequest.RecommendedPlaceSaveRequest> placesByContentId = requestedPlaces.stream()
                .collect(Collectors.toMap(
                        RecommendedCourseSaveRequest.RecommendedPlaceSaveRequest::contentId,
                        Function.identity(),
                        (first, second) -> first
                ));

        Map<String, TouristSpot> touristSpotsByContentId = touristSpotRepository
                .findByContentIdIn(placesByContentId.keySet()).stream()
                .collect(Collectors.toMap(TouristSpot::getContentId, Function.identity()));

        List<TouristSpot> missingTouristSpots = placesByContentId.entrySet().stream()
                .filter(entry -> !touristSpotsByContentId.containsKey(entry.getKey()))
                .map(entry -> toTouristSpot(entry.getValue()))
                .toList();

        if (!missingTouristSpots.isEmpty()) {
            touristSpotRepository.saveAll(missingTouristSpots)
                    .forEach(touristSpot -> touristSpotsByContentId.put(touristSpot.getContentId(), touristSpot));
        }

        return touristSpotsByContentId;
    }

    private TouristSpot toTouristSpot(RecommendedCourseSaveRequest.RecommendedPlaceSaveRequest place) {
        return TouristSpot.builder()
                .contentId(place.contentId())
                .contentTypeId(place.contentTypeId())
                .name(place.title())
                .legalDongRegionCode(place.legalDongRegionCode())
                .legalDongSigunguCode(place.legalDongSigunguCode())
                .address(place.address())
                .latitude(BigDecimal.valueOf(place.latitude()))
                .longitude(BigDecimal.valueOf(place.longitude()))
                .imageUrl(place.imageUrl())
                .build();
    }
}
