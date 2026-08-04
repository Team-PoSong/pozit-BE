package com.pozit.pozitserver.pozing.worker;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pozing.edit.worker.enabled", havingValue = "true")
public class PozingEditWorker {

    private static final String GROUP_NAME = "pozing-edit-workers";
    private static final String CONSUMER_NAME = "pozing-edit-worker-1";

    private final StringRedisTemplate stringRedisTemplate;
    private final PozingEditJobProcessor pozingEditJobProcessor;

    @PostConstruct
    public void initializeConsumerGroup() {
        try {
            RedisScript<Void> createGroupScript = RedisScript.of("""
                    redis.call('XGROUP', 'CREATE', KEYS[1], ARGV[1], '0', 'MKSTREAM')
                    return nil
                    """, Void.class);
            stringRedisTemplate.execute(
                    createGroupScript,
                    List.of(PozingEditQueuePublisher.STREAM_KEY),
                    GROUP_NAME
            );
        } catch (RedisSystemException | InvalidDataAccessApiUsageException e) {
            if (!Objects.toString(e.getMessage(), "").contains("BUSYGROUP")) {
                throw e;
            }
        }
    }

    @Scheduled(fixedDelayString = "${pozing.edit.worker.fixed-delay-ms:1000}")
    public void consumeOne() {
        List<MapRecord<String, Object, Object>> records = readRecords();

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            try {
                Long jobId = extractJobId(record);
                pozingEditJobProcessor.process(jobId);
            } catch (NumberFormatException e) {
                log.error("Invalid jobId in pozing edit record. recordId={}", record.getId(), e);
            } catch(Exception e){
                log.error("Failed to process pozing edit job. recordId={}", record.getId(), e);
                Long jobId = extractJobIdSafely(record);

                if (jobId != null) {
                    try {
                        pozingEditJobProcessor.markJobFailedInNewTransaction(jobId, e.getMessage());
                    } catch (Exception failException) {
                        log.error("Failed to mark pozing edit job as failed. jobId={}", jobId, failException);
                    }
                }
            }

            stringRedisTemplate.opsForStream().acknowledge(
                    PozingEditQueuePublisher.STREAM_KEY,
                    GROUP_NAME,
                    record.getId()
            );
        }
    }

    private List<MapRecord<String, Object, Object>> readRecords() {
        try {
            return stringRedisTemplate.opsForStream().read(
                    Consumer.from(GROUP_NAME, CONSUMER_NAME),
                    StreamReadOptions.empty().count(1).block(Duration.ofSeconds(1)),
                    StreamOffset.create(PozingEditQueuePublisher.STREAM_KEY, ReadOffset.lastConsumed())
            );
        } catch (RedisSystemException e) {
            if (Objects.toString(e.getMessage(), "").contains("NOGROUP")) {
                initializeConsumerGroup();
                return List.of();
            }
            throw e;
        }
    }

    private Long extractJobId(MapRecord<String, Object, Object> record) {
        Object jobId = record.getValue().get(PozingEditQueuePublisher.JOB_ID_FIELD);

        if (jobId == null) {
            throw new NumberFormatException("Missing jobId field.");
        }

        return Long.valueOf(jobId.toString());
    }

    private Long extractJobIdSafely(MapRecord<String, Object, Object> record) {
        try {
            return extractJobId(record);
        } catch (NumberFormatException e) {
            log.error("Invalid jobId in pozing edit record. recordId={}", record.getId(), e);
            return null;
        }
    }
}
