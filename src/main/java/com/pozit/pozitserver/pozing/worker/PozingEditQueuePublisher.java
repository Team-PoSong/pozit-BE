package com.pozit.pozitserver.pozing.worker;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PozingEditQueuePublisher {

    public static final String STREAM_KEY = "pozing-edit-jobs";
    public static final String JOB_ID_FIELD = "jobId";

    private final StringRedisTemplate stringRedisTemplate;

    public void publish(Long jobId) {
        MapRecord<String, String, String> record = StreamRecords.mapBacked(
                Map.of(JOB_ID_FIELD, jobId.toString())
        ).withStreamKey(STREAM_KEY);

        stringRedisTemplate.opsForStream().add(record);
    }
}
