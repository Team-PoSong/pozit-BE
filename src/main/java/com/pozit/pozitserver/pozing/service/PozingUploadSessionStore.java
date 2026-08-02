package com.pozit.pozitserver.pozing.service;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PozingUploadSessionStore {

    private static final String KEY_PREFIX = "pozing-upload:";
    private static final String USER_ID_FIELD = "userId";
    private static final String COURSE_SPOT_ID_FIELD = "courseSpotId";
    private static final String OBJECT_KEY_FIELD = "objectKey";

    private final StringRedisTemplate stringRedisTemplate;

    public void save(
            String uploadId,
            Long userId,
            Long courseSpotId,
            String objectKey,
            Duration ttl
    ) {
        String redisKey = createRedisKey(uploadId);
        stringRedisTemplate.opsForHash().putAll(redisKey, Map.of(
                USER_ID_FIELD, userId.toString(),
                COURSE_SPOT_ID_FIELD, courseSpotId.toString(),
                OBJECT_KEY_FIELD, objectKey
        ));
        stringRedisTemplate.expire(redisKey, ttl);
    }

    public PozingUploadSession get(String uploadId) {
        Map<Object, Object> values = stringRedisTemplate.opsForHash().entries(createRedisKey(uploadId));

        if (values.isEmpty()) {
            throw new BusinessException(ErrorCode.POZING_UPLOAD_SESSION_NOT_FOUND);
        }

        return new PozingUploadSession(
                Long.valueOf(requiredValue(values, USER_ID_FIELD)),
                Long.valueOf(requiredValue(values, COURSE_SPOT_ID_FIELD)),
                requiredValue(values, OBJECT_KEY_FIELD)
        );
    }

    public void delete(String uploadId) {
        stringRedisTemplate.delete(createRedisKey(uploadId));
    }

    private String requiredValue(Map<Object, Object> values, String field) {
        return Optional.ofNullable(values.get(field))
                .map(Object::toString)
                .orElseThrow(() -> new BusinessException(ErrorCode.POZING_UPLOAD_SESSION_NOT_FOUND));
    }

    private String createRedisKey(String uploadId) {
        return KEY_PREFIX + uploadId;
    }

    public record PozingUploadSession(
            Long userId,
            Long courseSpotId,
            String objectKey
    ) {
    }
}
