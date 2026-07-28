package com.pozit.pozitserver.like.service;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.like.domain.Like;
import com.pozit.pozitserver.like.repository.LikeRepository;
import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.travel.dto.response.PublicTravelListResponse;
import com.pozit.pozitserver.travel.repository.TravelMemberRepository;
import com.pozit.pozitserver.travel.repository.TravelRepository;
import com.pozit.pozitserver.travel.service.TravelService;
import com.pozit.pozitserver.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private static final String LIKE_UNIQUE_CONSTRAINT_NAME = "uk_like_travel_user";

    private final LikeRepository likeRepository;
    private final TravelRepository travelRepository;
    private final TravelMemberRepository travelMemberRepository;
    private final TravelService travelService;

    /**
     * 찜하기 (공개된 완료 여행만 가능, 본인이 참여한 여행은 불가)
     */
    @Transactional
    public void addLike(Long travelId, User user) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAVEL_NOT_FOUND));

        if (!travel.isPubliclyVisible()) {
            throw new BusinessException(ErrorCode.TRAVEL_NOT_FOUND);
        }

        if (travelMemberRepository.existsByTravelAndUser(travel, user)) {
            throw new BusinessException(ErrorCode.CANNOT_LIKE_OWN_TRAVEL);
        }

        if (likeRepository.existsByTravelAndUser(travel, user)) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        Like like = Like.builder()
                .travel(travel)
                .user(user)
                .build();
        
        try {
            likeRepository.saveAndFlush(like);
        } catch (DataIntegrityViolationException e) {
            if (!isLikeUniqueConstraintViolation(e)) {
                throw e;
            }
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }
    }

    private boolean isLikeUniqueConstraintViolation(DataIntegrityViolationException e) {
        Throwable cause = e.getMostSpecificCause();
        return cause.getMessage() != null && cause.getMessage().contains(LIKE_UNIQUE_CONSTRAINT_NAME);
    }

    /**
     * 찜 해제
     */
    @Transactional
    public void deleteLike(Long travelId, User user) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAVEL_NOT_FOUND));

        Like like = likeRepository.findByTravelAndUser(travel, user)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIKE_NOT_FOUND));

        likeRepository.delete(like);
    }

    /**
     * 찜 목록 조회 (최근 찜한 순, 찜 이후 비공개로 전환된 여행은 제외)
     */
    public List<PublicTravelListResponse> getLikes(User user) {
        List<Travel> travels = likeRepository.findAllWithTravelByUser(user).stream()
                .map(Like::getTravel)
                .filter(Travel::isPubliclyVisible)
                .toList();

        return travelService.buildPublicTravelListResponses(travels, user);
    }
}
