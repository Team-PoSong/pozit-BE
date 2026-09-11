package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.recommendation.dto.RecommendedCourseResponse;
import com.pozit.pozitserver.travel.dto.request.TravelCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RecommendationPreviewStore {

    public static final long TTL_SECONDS = 1800;

    private static final String KEY_PREFIX = "recommendation:preview:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public String saveDraft(Long userId, TravelCreateRequest travelCreateRequest, RecommendedCourseResponse recommendedCourse) {
        String previewId = UUID.randomUUID().toString();
        PreviewPayload payload = new PreviewPayload(userId, travelCreateRequest, recommendedCourse);

        savePayload(previewId, payload);

        return previewId;
    }

    private void savePayload(String previewId, PreviewPayload payload) {
        try {
            stringRedisTemplate.opsForValue().set(
                    key(previewId),
                    objectMapper.writeValueAsString(payload),
                    Duration.ofSeconds(TTL_SECONDS)
            );
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.COMMON500);
        }
    }

    public DraftPreview findDraft(String previewId, Long userId) {
        PreviewPayload payload = findPayload(previewId);
        validateOwner(payload, userId);

        if (payload.travelCreateRequest() == null || payload.recommendedCourse() == null) {
            throw new BusinessException(ErrorCode.COMMON404);
        }

        return new DraftPreview(payload.travelCreateRequest(), payload.recommendedCourse());
    }

    public void delete(String previewId) {
        stringRedisTemplate.delete(key(previewId));
    }

    private PreviewPayload findPayload(String previewId) {
        String payloadJson = stringRedisTemplate.opsForValue().get(key(previewId));
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON404);
        }

        try {
            return objectMapper.readValue(payloadJson, PreviewPayload.class);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.COMMON500);
        }
    }

    private void validateOwner(PreviewPayload payload, Long userId) {
        if (!Objects.equals(payload.userId(), userId)) {
            throw new BusinessException(ErrorCode.COMMON403);
        }
    }

    private String key(String previewId) {
        return KEY_PREFIX + previewId;
    }

    private record PreviewPayload(
            Long userId,
            TravelCreateRequest travelCreateRequest,
            RecommendedCourseResponse recommendedCourse
    ) {
    }

    public record DraftPreview(
            TravelCreateRequest travelCreateRequest,
            RecommendedCourseResponse recommendedCourse
    ) {
    }
}
