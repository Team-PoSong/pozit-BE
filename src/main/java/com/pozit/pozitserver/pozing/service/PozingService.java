package com.pozit.pozitserver.pozing.service;

import com.pozit.pozitserver.course.domain.CourseSpot;
import com.pozit.pozitserver.course.domain.CourseSpotStatus;
import com.pozit.pozitserver.course.repository.CourseSpotRepository;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.global.s3.S3Service;
import com.pozit.pozitserver.pozing.domain.Pozing;
import com.pozit.pozitserver.pozing.dto.request.PozingSaveRequest;
import com.pozit.pozitserver.pozing.dto.response.PozingSaveResponse;
import com.pozit.pozitserver.pozing.repository.PozingRepository;
import com.pozit.pozitserver.travel.dto.response.PresignedUrlResponse;
import com.pozit.pozitserver.travel.repository.TravelMemberRepository;
import com.pozit.pozitserver.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PozingService {

    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofMinutes(10);
    private static final String POZING_VIDEO_CONTENT_TYPE = "video/mp4";

    private final S3Service s3Service;
    private final CourseSpotRepository courseSpotRepository;
    private final PozingRepository pozingRepository;
    private final TravelMemberRepository travelMemberRepository;

    /**
     * 타임랩스 저장용 presigned url 발급
     */
    public PresignedUrlResponse getPozingPresignedUrl(User user,Long courseSpotId){

        String key = "pozings/%d/%d/%s.mp4".formatted(
                user.getId(),
                courseSpotId,
                UUID.randomUUID()
        );

        return s3Service.createPutPresignedUrl(
                key,
                POZING_VIDEO_CONTENT_TYPE,
                PRESIGNED_URL_EXPIRATION
        );
    }

    /**
     * pozing S3 url을 DB에 저장
     */
    @Transactional
    public PozingSaveResponse savePozing(
            User user,
            PozingSaveRequest request
    ) {
        CourseSpot courseSpot = courseSpotRepository.findById(request.courseSpotId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_SPOT_NOT_FOUND));

        validateMember(courseSpot, user);

        Pozing pozing = pozingRepository.save(
                Pozing.builder()
                        .courseSpot(courseSpot)
                        .user(user)
                        .pozingUrl(request.pozingUrl())
                        .thumbnailUrl(request.thumbnailUrl())
                        .build()
        );

        //모든 멤버들이 포징 업데이트 완료 시 VISITED로 상태 변경
        updateCourseSpotStatusIfAllMembersSaved(courseSpot);

        return new PozingSaveResponse(
                pozing.getId(),
                courseSpot.getId(),
                pozing.getPozingUrl(),
                pozing.getThumbnailUrl()
        );
    }

    private void validateMember(CourseSpot courseSpot, User user) {
        boolean isMember = travelMemberRepository.existsByTravelAndUser(
                courseSpot.getCourse().getTravel(),
                user
        );

        if (!isMember) {
            throw new BusinessException(ErrorCode.NOT_VALID_TRAVEL_MEMBER);
        }
    }

    private void updateCourseSpotStatusIfAllMembersSaved(CourseSpot courseSpot) {
        long memberCount = travelMemberRepository.countByTravel(
                courseSpot.getCourse().getTravel()
        );
        long savedUserCount = pozingRepository.countDistinctUserByCourseSpot(courseSpot);

        if (memberCount > 0 && savedUserCount >= memberCount) {
            courseSpot.updateStatus(CourseSpotStatus.VISITED);
        }
    }
}
