package com.pozit.pozitserver.travel.service;

import com.pozit.pozitserver.course.domain.Course;
import com.pozit.pozitserver.course.domain.CourseSpot;
import com.pozit.pozitserver.course.domain.TouristSpot;
import com.pozit.pozitserver.course.repository.CourseRepository;
import com.pozit.pozitserver.course.repository.CourseSpotRepository;
import com.pozit.pozitserver.course.repository.TouristSpotRepository;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.tag.domain.Tag;
import com.pozit.pozitserver.tag.domain.TravelTag;
import com.pozit.pozitserver.tag.repository.TagRepository;
import com.pozit.pozitserver.tag.repository.TravelTagRepository;
import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.travel.domain.TravelMember;
import com.pozit.pozitserver.travel.domain.TravelMemberRole;
import com.pozit.pozitserver.travel.dto.request.LikeBasedTravelCreateRequest;
import com.pozit.pozitserver.travel.dto.response.LikeBasedTravelDraftResponse;
import com.pozit.pozitserver.travel.dto.response.TravelCreateResponse;
import com.pozit.pozitserver.travel.repository.TravelMemberRepository;
import com.pozit.pozitserver.travel.repository.TravelRepository;
import com.pozit.pozitserver.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeTravelService {

    private final TravelRepository travelRepository;
    private final TravelService travelService;
    private final TravelMemberRepository travelMemberRepository;
    private final TravelTagRepository travelTagRepository;
    private final TagRepository tagRepository;
    private final CourseRepository courseRepository;
    private final CourseSpotRepository courseSpotRepository;
    private final TouristSpotRepository touristSpotRepository;

    public LikeBasedTravelDraftResponse getLikeBasedTravelDraft(Long sourceTravelId) {
        Travel sourceTravel = findPublicSourceTravel(sourceTravelId);

        List<TravelTag> travelTags = travelTagRepository.findAllWithTagByTravelId(sourceTravel.getId());
        List<Long> tagIds = travelTags.stream()
                .map(travelTag -> travelTag.getTag().getId())
                .toList();

        List<Course> courses = courseRepository.findByTravelOrderByDayNumberAsc(sourceTravel);
        List<CourseSpot> courseSpots = courses.isEmpty()
                ? List.of()
                : courseSpotRepository.findAllByCourseInOrder(courses);

        Map<Long, List<CourseSpot>> spotsByCourseId = courseSpots.stream()
                .collect(Collectors.groupingBy(
                        spot -> spot.getCourse().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<LikeBasedTravelDraftResponse.CourseDraft> courseDrafts = courses.stream()
                .map(course -> new LikeBasedTravelDraftResponse.CourseDraft(
                        course.getId(),
                        course.getDayNumber(),
                        course.getDate(),
                        spotsByCourseId.getOrDefault(course.getId(), List.of()).stream()
                                .map(this::toCourseSpotDraft)
                                .toList()
                ))
                .toList();

        return new LikeBasedTravelDraftResponse(
                sourceTravel.getId(),
                sourceTravel.getTitle(),
                sourceTravel.getDestination(),
                sourceTravel.getRegionCode(),
                sourceTravel.getStartDate(),
                sourceTravel.getEndDate(),
                sourceTravel.getTransportation(),
                sourceTravel.getTravelStyle(),
                sourceTravel.getBackgroundImageUrl(),
                tagIds,
                courseDrafts
        );
    }

    @Transactional
    public TravelCreateResponse createTravelFromLikeDraft(
            User user,
            LikeBasedTravelCreateRequest request
    ) {
        Travel sourceTravel = findPublicSourceTravel(request.sourceTravelId());
        validateTravelPeriod(request);
        validateCourses(request);

        List<Tag> tags = findTags(request.tagIds());
        Map<Long, TouristSpot> touristSpotMap = findTouristSpots(request);

        String inviteCode = travelService.generateUniqueInviteCode();

        Travel travel = Travel.builder()
                .leader(user)
                .title(request.title())
                .destination(sourceTravel.getDestination())
                .regionCode(sourceTravel.getRegionCode())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .transportation(request.transportation())
                .travelStyle(request.travelStyle())
                .inviteCode(inviteCode)
                .build();
        travel.updateBackgroundImage(request.backgroundImageUrl());

        Travel savedTravel = travelRepository.save(travel);
        saveLeader(savedTravel, user);
        saveTags(savedTravel, tags);
        List<Course> savedCourses = saveCoursesAndSpots(savedTravel, request, touristSpotMap);

        return TravelCreateResponse.from(savedTravel, savedCourses);
    }

    private LikeBasedTravelDraftResponse.CourseSpotDraft toCourseSpotDraft(CourseSpot courseSpot) {
        TouristSpot touristSpot = courseSpot.getTouristSpot();

        return new LikeBasedTravelDraftResponse.CourseSpotDraft(
                courseSpot.getId(),
                touristSpot.getId(),
                touristSpot.getName(),
                touristSpot.getAddress(),
                touristSpot.getImageUrl(),
                courseSpot.getOrderIndex()
        );
    }

    private Travel findPublicSourceTravel(Long sourceTravelId) {
        Travel sourceTravel = travelRepository.findById(sourceTravelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAVEL_NOT_FOUND));

        if (!sourceTravel.isPubliclyVisible()) {
            throw new BusinessException(ErrorCode.TRAVEL_NOT_FOUND);
        }

        return sourceTravel;
    }

    private void validateTravelPeriod(LikeBasedTravelCreateRequest request) {
        long nights = ChronoUnit.DAYS.between(request.startDate(), request.endDate());

        if (nights < 0 || nights > 3) {
            throw new BusinessException(ErrorCode.INVALID_TRAVEL_PERIOD);
        }
    }

    private void validateCourses(LikeBasedTravelCreateRequest request) {
        int dayCount = (int) ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        Set<Integer> dayNumbers = new HashSet<>();

        for (LikeBasedTravelCreateRequest.CourseRequest course : request.courses()) {
            if (course.dayNumber() < 1 || course.dayNumber() > dayCount) {
                throw new BusinessException(ErrorCode.COMMON400);
            }

            if (!dayNumbers.add(course.dayNumber())) {
                throw new BusinessException(ErrorCode.COMMON400);
            }

            Set<Long> touristSpotIds = new HashSet<>(course.touristSpotIds());
            if (touristSpotIds.size() != course.touristSpotIds().size()) {
                throw new BusinessException(ErrorCode.DUPLICATE_COURSE_SPOT);
            }
        }
    }

    private List<Tag> findTags(List<Long> tagIds) {
        List<Long> distinctTagIds = tagIds.stream()
                .distinct()
                .toList();
        List<Tag> tags = tagRepository.findAllById(distinctTagIds);

        if (tags.size() != distinctTagIds.size()) {
            throw new BusinessException(ErrorCode.COMMON404);
        }

        return tags;
    }

    private Map<Long, TouristSpot> findTouristSpots(LikeBasedTravelCreateRequest request) {
        List<Long> touristSpotIds = request.courses().stream()
                .flatMap(course -> course.touristSpotIds().stream())
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));

        List<TouristSpot> touristSpots = touristSpotRepository.findAllById(touristSpotIds);

        if (touristSpots.size() != touristSpotIds.size()) {
            throw new BusinessException(ErrorCode.COMMON404);
        }

        return touristSpots.stream()
                .collect(Collectors.toMap(TouristSpot::getId, Function.identity()));
    }

    private void saveLeader(Travel travel, User user) {
        TravelMember leaderMember = TravelMember.builder()
                .travel(travel)
                .user(user)
                .role(TravelMemberRole.LEADER)
                .build();
        travelMemberRepository.save(leaderMember);
    }

    private void saveTags(Travel travel, List<Tag> tags) {
        List<TravelTag> travelTags = tags.stream()
                .map(tag -> TravelTag.create(travel, tag))
                .toList();
        travelTagRepository.saveAll(travelTags);
    }

    private List<Course> saveCoursesAndSpots(
            Travel travel,
            LikeBasedTravelCreateRequest request,
            Map<Long, TouristSpot> touristSpotMap
    ) {
        List<Course> courses = request.courses().stream()
                .map(courseRequest -> Course.builder()
                        .travel(travel)
                        .dayNumber(courseRequest.dayNumber())
                        .date(travel.getStartDate().plusDays(courseRequest.dayNumber() - 1L))
                        .build())
                .toList();
        List<Course> savedCourses = courseRepository.saveAll(courses);

        Map<Integer, Course> courseByDayNumber = savedCourses.stream()
                .collect(Collectors.toMap(Course::getDayNumber, Function.identity()));

        List<CourseSpot> courseSpots = new ArrayList<>();
        for (LikeBasedTravelCreateRequest.CourseRequest courseRequest : request.courses()) {
            Course course = courseByDayNumber.get(courseRequest.dayNumber());

            for (int i = 0; i < courseRequest.touristSpotIds().size(); i++) {
                Long touristSpotId = courseRequest.touristSpotIds().get(i);
                courseSpots.add(CourseSpot.builder()
                        .course(course)
                        .touristSpot(touristSpotMap.get(touristSpotId))
                        .orderIndex(i + 1)
                        .build());
            }
        }

        courseSpotRepository.saveAll(courseSpots);
        return savedCourses;
    }
}
