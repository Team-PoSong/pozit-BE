package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.recommendation.dto.RecommendedCourseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RecommendationPreviewStore {

    public static final long TTL_SECONDS = 1800;

    private static final String KEY_PREFIX = "recommendation:preview:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public String save(Long travelId, Long userId, RecommendedCourseResponse recommendedCourse) {
        String previewId = UUID.randomUUID().toString();
        PreviewPayload payload = new PreviewPayload(travelId, userId, recommendedCourse);

        try {
            stringRedisTemplate.opsForValue().set(
                    key(previewId),
                    objectMapper.writeValueAsString(payload),
                    Duration.ofSeconds(TTL_SECONDS)
            );
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.COMMON500);
        }

        return previewId;
    }

    public RecommendedCourseResponse find(String previewId, Long travelId, Long userId) {
        String payloadJson = stringRedisTemplate.opsForValue().get(key(previewId));
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON404);
        }

        PreviewPayload payload;
        try {
            payload = objectMapper.readValue(payloadJson, PreviewPayload.class);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.COMMON500);
        }

        if (!payload.travelId().equals(travelId)) {
            throw new BusinessException(ErrorCode.COMMON404);
        }
        if (!payload.userId().equals(userId)) {
            throw new BusinessException(ErrorCode.COMMON403);
        }

        return payload.recommendedCourse();
    }

    private String key(String previewId) {
        return KEY_PREFIX + previewId;
    }

    private record PreviewPayload(
            Long travelId,
            Long userId,
            RecommendedCourseResponse recommendedCourse
    ) {
    }
}
