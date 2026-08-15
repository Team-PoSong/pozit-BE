package com.pozit.pozitserver.pozing.worker.thumbnail;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PozingThumbnailQueuePublisher {

    public static final String STREAM_KEY = "pozing-thumbnail-jobs";
    public static final String POZING_ID_FIELD = "pozingId";

    private final StringRedisTemplate stringRedisTemplate;

    public void publish(Long pozingId) {
        MapRecord<String, String, String> record = StreamRecords.mapBacked(
                Map.of(POZING_ID_FIELD, pozingId.toString())
        ).withStreamKey(STREAM_KEY);

        stringRedisTemplate.opsForStream().add(record);
    }
}
