package com.pozit.pozitserver.travel.service;

import com.pozit.pozitserver.course.domain.Course;
import com.pozit.pozitserver.course.domain.CourseSpot;
import com.pozit.pozitserver.course.domain.CourseSpotStatus;
import com.pozit.pozitserver.course.repository.CourseRepository;
import com.pozit.pozitserver.course.repository.CourseSpotRepository;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.global.util.RandomUtil;
import com.pozit.pozitserver.pozing.domain.Pozing;
import com.pozit.pozitserver.pozing.repository.PozingRepository;
import com.pozit.pozitserver.tag.domain.Tag;
import com.pozit.pozitserver.tag.domain.TravelTag;
import com.pozit.pozitserver.tag.dto.response.TagResponse;
import com.pozit.pozitserver.tag.repository.TagRepository;
import com.pozit.pozitserver.tag.repository.TravelTagRepository;
import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.travel.domain.TravelMember;
import com.pozit.pozitserver.travel.domain.TravelMemberRole;
import com.pozit.pozitserver.travel.domain.TravelStatus;
import com.pozit.pozitserver.travel.dto.request.TravelCreateRequest;
import com.pozit.pozitserver.travel.dto.request.TravelJoinRequest;
import com.pozit.pozitserver.travel.dto.request.TravelUpdateRequest;
import com.pozit.pozitserver.travel.dto.request.TravelVisibilityRequest;
import com.pozit.pozitserver.travel.dto.response.InviteCodeResponse;
import com.pozit.pozitserver.travel.dto.response.JoinResponse;
import com.pozit.pozitserver.travel.dto.response.PublicTravelDetailResponse;
import com.pozit.pozitserver.travel.dto.response.TravelCreateResponse;
import com.pozit.pozitserver.travel.dto.response.TravelDetailResponse;
import com.pozit.pozitserver.travel.dto.response.TravelJoinResponse;
import com.pozit.pozitserver.travel.dto.response.TravelListResponse;
import com.pozit.pozitserver.travel.repository.TravelMemberRepository;
import com.pozit.pozitserver.travel.repository.TravelRepository;
import com.pozit.pozitserver.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelService {

    private static final int SEARCH_LIMIT = 10;

    private final TravelRepository travelRepository;
    private final TravelMemberRepository travelMemberRepository;
    private final TravelTagRepository travelTagRepository;
    private final TagRepository tagRepository;
    private final CourseRepository courseRepository;
    private final CourseSpotRepository courseSpotRepository;
    private final PozingRepository pozingRepository;


    /**
     * 여행 생성
     */
    @Transactional
    public TravelCreateResponse makeTravel(TravelCreateRequest request, User user) {
        List<Long> distinctTagIds=request.tagIds()
                .stream()
                .distinct()
                .toList();
        List<Tag> tags=tagRepository.findAllById(distinctTagIds);

        String inviteCode = generateUniqueInviteCode();

        Travel travel=Travel.builder()
                .leader(user)
                .title(request.title())
                .destination(request.destination())
                .regionCode(request.regionCode())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .inviteCode(inviteCode)
                .build();
        Travel savedTravel=travelRepository.save(travel);

        List<TravelTag> travelTags=tags.stream()
                .map(tag->TravelTag.create(savedTravel,tag))
                .toList();
        travelTagRepository.saveAll(travelTags);

        TravelMember travelMember=TravelMember.builder()
                .travel(savedTravel)
                .user(user)
                .role(TravelMemberRole.LEADER)
                .build();
        travelMemberRepository.save(travelMember);
        return TravelCreateResponse.from(savedTravel);
    }

    private String generateUniqueInviteCode(){
        for (int i = 0; i < SEARCH_LIMIT; i++) {
            String inviteCode = RandomUtil.generateInviteCode();
            if (!travelRepository.existsByInviteCode(inviteCode)) {
                return inviteCode;
            }
        }

        throw new BusinessException(ErrorCode.INVITE_CODE_GENERATION_FAILED);
    }

    /**
     * invite code 조회
     */
    public InviteCodeResponse getInviteCode(Long travelId){
        Travel travel=travelRepository.findById(travelId)
                .orElseThrow(()->new BusinessException(ErrorCode.TRAVEL_NOT_FOUND));
        return new InviteCodeResponse(travelId,travel.getInviteCode());
    }

    /**
     * Invite Code를 통한 여행 탐색
     */
    public TravelJoinResponse findTravel(TravelJoinRequest request, User user){
        Travel travel=travelRepository.findByInviteCode(request.inviteCode())
                .orElseThrow(()->new BusinessException(ErrorCode.INVALID_INVITE_CODE));
//        validateJoinable(travel,user);

        Long memberCount=travelMemberRepository.countByTravel(travel);
        List<String> tags=travelTagRepository.findTagNamesByTravelId(travel.getId());

        boolean alreadyJoined=travelMemberRepository.existsByTravelAndUser(travel,user);
        if(alreadyJoined){
            return TravelJoinResponse.joined(travel, memberCount, tags);
        }

        return TravelJoinResponse.from(travel, memberCount, tags);
    }

    /**
     * 여행 참여
     */
    @Transactional
    public JoinResponse joinTravel(Long travelId, User user){
        Travel travel=travelRepository.findById(travelId)
                .orElseThrow(()->new BusinessException(ErrorCode.TRAVEL_NOT_FOUND));

        TravelMember travelMember=TravelMember.builder()
                .travel(travel)
                .user(user)
                .role(TravelMemberRole.MEMBER)
                .build();

        travelMemberRepository.save(travelMember);
        return JoinResponse.from(travel,travelMember);
    }


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

    public List<TagResponse> getTravelTags(User currentUser, Long travelId) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAVEL_NOT_FOUND));

        validateMember(travel, currentUser);

        return travelTagRepository.findAllWithTagByTravelId(travelId)
                .stream()
                .map(travelTag -> TagResponse.from(travelTag.getTag()))
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

        return buildTravelDetailResponse(travel);
    }

    /**
     * 공개 여행 피드 조회 (완료 + 공개 여행만, 로그인 시 본인이 참여한 여행은 제외)
     * region/startDate·endDate/tagIds/keyword로 추가 검색·필터링이 가능
     */
    public List<TravelListResponse> getPublicTravels(
            User currentUser,
            String region,
            LocalDate startDate,
            LocalDate endDate,
            List<Long> tagIds,
            String keyword
    ) {
        List<Travel> travels = travelRepository.searchPublicTravels(region, startDate, endDate, tagIds, keyword);

        if (currentUser != null) {
            Set<Long> myTravelIds = travelMemberRepository.findAllWithTravelByUser(currentUser).stream()
                    .map(m -> m.getTravel().getId())
                    .collect(Collectors.toSet());
            travels = travels.stream()
                    .filter(travel -> !myTravelIds.contains(travel.getId()))
                    .toList();
        }

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

    /**
     * 공개 여행 상세 조회 (완료 + 공개 여행만, 비로그인 접근 가능)
     * 초대 코드와 멤버 개인 정보(userId)는 노출하지 않는 전용 응답을 사용한다.
     */
    public PublicTravelDetailResponse getPublicTravelDetail(Long travelId) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAVEL_NOT_FOUND));

        validatePublicDone(travel);

        return buildPublicTravelDetailResponse(travel);
    }

    private void validatePublicDone(Travel travel) {
        if (!travel.isPubliclyVisible()) {
            throw new BusinessException(ErrorCode.TRAVEL_NOT_FOUND);
        }
    }

    private PublicTravelDetailResponse buildPublicTravelDetailResponse(Travel travel) {
        TravelAggregate aggregate = collectTravelAggregate(travel);

        List<PublicTravelDetailResponse.CourseInfo> courseInfos = aggregate.courses().stream()
                .map(course -> toPublicCourseInfo(course, aggregate.spotsByCourseId(), aggregate.pozingsByCourseSpotId()))
                .toList();

        return new PublicTravelDetailResponse(
                travel.getId(),
                travel.getTitle(),
                travel.getDestination(),
                travel.getStartDate(),
                travel.getEndDate(),
                travel.getStatus().name(),
                travel.getIsPublic(),
                travel.getBackgroundImageUrl(),
                aggregate.leaderNickname(),
                aggregate.members().size(),
                aggregate.completionRate(),
                aggregate.totalSpotCount(),
                aggregate.totalPozingCount(),
                aggregate.tags(),
                courseInfos
        );
    }

    private PublicTravelDetailResponse.CourseInfo toPublicCourseInfo(
            Course course,
            Map<Long, List<CourseSpot>> spotsByCourseId,
            Map<Long, List<Pozing>> pozingsByCourseSpotId
    ) {
        List<CourseSpot> spots = spotsByCourseId.getOrDefault(course.getId(), List.of());

        List<PublicTravelDetailResponse.CourseSpotInfo> spotInfos = spots.stream()
                .map(spot -> {
                    List<Pozing> pozings = pozingsByCourseSpotId.getOrDefault(spot.getId(), List.of());
                    List<PublicTravelDetailResponse.PublicPozingInfo> pozingInfos = pozings.stream()
                            .map(p -> new PublicTravelDetailResponse.PublicPozingInfo(
                                    p.getId(),
                                    p.getUser().getNickname(),
                                    p.getPozingUrl(),
                                    p.getThumbnailUrl()
                            ))
                            .toList();

                    return new PublicTravelDetailResponse.CourseSpotInfo(
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

        return new PublicTravelDetailResponse.CourseInfo(
                course.getId(),
                course.getDayNumber(),
                course.getDate(),
                spotInfos
        );
    }

    private TravelDetailResponse buildTravelDetailResponse(Travel travel) {
        TravelAggregate aggregate = collectTravelAggregate(travel);

        List<TravelDetailResponse.CourseInfo> courseInfos = aggregate.courses().stream()
                .map(course -> toCourseInfo(course, aggregate.spotsByCourseId(), aggregate.pozingsByCourseSpotId()))
                .toList();

        List<TravelDetailResponse.MemberInfo> memberInfos = aggregate.members().stream()
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
                aggregate.completionRate(),
                aggregate.totalSpotCount(),
                aggregate.totalPozingCount(),
                aggregate.tags(),
                memberInfos,
                courseInfos
        );
    }

    /**
     * 여행 상세 조립에 필요한 멤버/태그/코스/스팟/포징 데이터를 한 번에 조회하고 그룹핑한다.
     * TravelDetailResponse, PublicTravelDetailResponse 조립에서 공통으로 사용한다.
     */
    private record TravelAggregate(
            List<Course> courses,
            Map<Long, List<CourseSpot>> spotsByCourseId,
            Map<Long, List<Pozing>> pozingsByCourseSpotId,
            List<TravelMember> members,
            List<String> tags,
            int totalSpotCount,
            int totalPozingCount,
            int completionRate,
            String leaderNickname
    ) {}

    private TravelAggregate collectTravelAggregate(Travel travel) {
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

        String leaderNickname = members.stream()
                .filter(m -> m.getRole() == TravelMemberRole.LEADER)
                .findFirst()
                .map(m -> m.getUser().getNickname())
                .orElse(null);

        return new TravelAggregate(
                courses,
                spotsByCourseId,
                pozingsByCourseSpotId,
                members,
                tags,
                allSpots.size(),
                allPozings.size(),
                calculateCompletionRate(allSpots),
                leaderNickname
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

    /**
     * 여행 정보 수정 (리더만 가능, DONE 상태는 날짜 변경 불가)
     */
    @Transactional
    public void updateTravel(User currentUser, Long travelId, TravelUpdateRequest request) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON404));

        validateLeader(travel, currentUser);
        validateDateChangeAllowed(travel, request.startDate(), request.endDate());

        LocalDate oldStartDate = travel.getStartDate();
        LocalDate oldEndDate = travel.getEndDate();

        travel.updateInfo(request.title(), request.destination(), request.startDate(), request.endDate());
        syncCourseDates(travel, oldStartDate, oldEndDate, request.startDate(), request.endDate());
        replaceTags(travel, request.tagIds());
    }

    /**
     * 여행 공개 설정 변경 (리더만 가능, 완료된 여행만 가능)
     */
    @Transactional
    public void updateVisibility(User currentUser, Long travelId, TravelVisibilityRequest request) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON404));

        validateLeader(travel, currentUser);

        if (travel.getStatus() != TravelStatus.DONE) {
            throw new BusinessException(ErrorCode.TRAVEL_NOT_COMPLETED);
        }

        travel.updateVisibility(request.isPublic());
    }

    private void validateDateChangeAllowed(Travel travel, LocalDate startDate, LocalDate endDate) {
        if (travel.getStatus() != TravelStatus.DONE) {
            return;
        }

        if (!travel.getStartDate().equals(startDate) || !travel.getEndDate().equals(endDate)) {
            throw new BusinessException(ErrorCode.COMPLETED_TRAVEL_DATE_NOT_EDITABLE);
        }
    }

    /**
     * 여행 기간 변경에 맞춰 코스를 동기화
     */
    private void syncCourseDates(
            Travel travel,
            LocalDate oldStartDate,
            LocalDate oldEndDate,
            LocalDate newStartDate,
            LocalDate newEndDate
    ) {
        int oldDayCount = (int) (ChronoUnit.DAYS.between(oldStartDate, oldEndDate) + 1);
        int newDayCount = (int) (ChronoUnit.DAYS.between(newStartDate, newEndDate) + 1);

        List<Course> courses = courseRepository.findByTravelOrderByDayNumberAsc(travel);

        for (Course course : courses) {
            if (course.getDayNumber() <= newDayCount) {
                course.updateDate(newStartDate.plusDays(course.getDayNumber() - 1));
            }
        }

        if (newDayCount > oldDayCount) {
            List<Course> newCourses = new ArrayList<>();
            for (int day = oldDayCount + 1; day <= newDayCount; day++) {
                newCourses.add(Course.builder()
                        .travel(travel)
                        .dayNumber(day)
                        .date(newStartDate.plusDays(day - 1))
                        .build());
            }
            courseRepository.saveAll(newCourses);
        } else if (newDayCount < oldDayCount) {
            List<Course> coursesToRemove = courses.stream()
                    .filter(course -> course.getDayNumber() > newDayCount)
                    .toList();

            if (!coursesToRemove.isEmpty()) {
                List<CourseSpot> spotsToRemove = courseSpotRepository.findAllByCourseInOrder(coursesToRemove);
                if (!spotsToRemove.isEmpty()) {
                    pozingRepository.deleteAllInBatch(pozingRepository.findByCourseSpotIn(spotsToRemove));
                    courseSpotRepository.deleteAllInBatch(spotsToRemove);
                }
                courseRepository.deleteAllInBatch(coursesToRemove);
            }
        }
    }

    private void replaceTags(Travel travel, List<Long> tagIds) {
        travelTagRepository.deleteAllInBatch(travelTagRepository.findByTravel(travel));

        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }

        Set<Long> uniqueTagIds = new HashSet<>(tagIds);
        List<Tag> tags = tagRepository.findAllById(uniqueTagIds);
        if (tags.size() != uniqueTagIds.size()) {
            throw new BusinessException(ErrorCode.COMMON404);
        }

        List<TravelTag> travelTags = tags.stream()
                .map(tag -> TravelTag.builder().travel(travel).tag(tag).build())
                .toList();
        travelTagRepository.saveAll(travelTags);
    }

    private void validateLeader(Travel travel, User user) {
        TravelMember member = travelMemberRepository.findByTravelAndUser(travel, user)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON403));

        if (member.getRole() != TravelMemberRole.LEADER) {
            throw new BusinessException(ErrorCode.COMMON403);
        }
    }
}
