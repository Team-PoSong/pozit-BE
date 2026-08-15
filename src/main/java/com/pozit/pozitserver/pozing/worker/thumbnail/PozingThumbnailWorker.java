package com.pozit.pozitserver.pozing.worker.thumbnail;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
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
@ConditionalOnProperty(name = "pozing.thumbnail.worker.enabled", havingValue = "true")
public class PozingThumbnailWorker {

    private static final String GROUP_NAME = "pozing-thumbnail-workers";
    private static final String CONSUMER_NAME = "pozing-thumbnail-worker-1";

    private final StringRedisTemplate stringRedisTemplate;
    private final PozingThumbnailJobProcessor pozingThumbnailJobProcessor;

    @PostConstruct
    public void initializeConsumerGroup() {
        try {
            RedisScript<Void> createGroupScript = RedisScript.of("""
                    redis.call('XGROUP', 'CREATE', KEYS[1], ARGV[1], '0', 'MKSTREAM')
                    return nil
                    """, Void.class);
            stringRedisTemplate.execute(
                    createGroupScript,
                    List.of(PozingThumbnailQueuePublisher.STREAM_KEY),
                    GROUP_NAME
            );
        } catch (RedisSystemException | InvalidDataAccessApiUsageException e) {
            if (!Objects.toString(e.getMostSpecificCause().getMessage(), "").contains("BUSYGROUP")) {
                throw e;
            }
        }
    }

    @Scheduled(fixedDelayString = "${pozing.thumbnail.worker.fixed-delay-ms:1000}")
    public void consumeOne() {
        List<MapRecord<String, Object, Object>> records = readRecords();

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            try {
                Long pozingId = extractPozingId(record);
                pozingThumbnailJobProcessor.process(pozingId);
            } catch (NumberFormatException e) {
                log.error("Invalid pozingId in pozing thumbnail record. recordId={}", record.getId(), e);
            } catch (Exception e) {
                log.error("Failed to process pozing thumbnail job. recordId={}", record.getId(), e);
                Long pozingId = extractPozingIdSafely(record);

                if (pozingId != null) {
                    try {
                        pozingThumbnailJobProcessor.markJobFailedInNewTransaction(pozingId);
                    } catch (Exception failException) {
                        log.error("Failed to mark pozing thumbnail job as failed. pozingId={}", pozingId, failException);
                    }
                }
            }

            stringRedisTemplate.opsForStream().acknowledge(
                    PozingThumbnailQueuePublisher.STREAM_KEY,
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
                    StreamOffset.create(PozingThumbnailQueuePublisher.STREAM_KEY, ReadOffset.lastConsumed())
            );
        } catch (RedisSystemException e) {
            if (Objects.toString(e.getMostSpecificCause().getMessage(), "").contains("NOGROUP")) {
                initializeConsumerGroup();
                return List.of();
            }
            throw e;
        }
    }

    private Long extractPozingId(MapRecord<String, Object, Object> record) {
        Object pozingId = record.getValue().get(PozingThumbnailQueuePublisher.POZING_ID_FIELD);

        if (pozingId == null) {
            throw new NumberFormatException("Missing pozingId field.");
        }

        return Long.valueOf(pozingId.toString());
    }

    private Long extractPozingIdSafely(MapRecord<String, Object, Object> record) {
        try {
            return extractPozingId(record);
        } catch (NumberFormatException e) {
            log.error("Invalid pozingId in pozing thumbnail record. recordId={}", record.getId(), e);
            return null;
        }
    }
}
