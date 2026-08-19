package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.recommendation.dto.CourseChatRequest;
import com.pozit.pozitserver.recommendation.dto.CourseChatResponse;
import com.pozit.pozitserver.user.domain.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Service
public class CourseChatStreamService {

    private static final long TIMEOUT_MILLIS = 120_000L;
    private static final long MESSAGE_DELAY_MILLIS = 25L;

    private final CourseChatService courseChatService;
    private final TaskExecutor taskExecutor;

    public CourseChatStreamService(
            CourseChatService courseChatService,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor
    ) {
        this.courseChatService = courseChatService;
        this.taskExecutor = taskExecutor;
    }

    public SseEmitter stream(Long travelId, User currentUser, CourseChatRequest request) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);

        taskExecutor.execute(() -> {
            try {
                sendEvent(emitter, "status", "코스 수정 요청을 분석하고 있어요.");
                CourseChatResponse response = courseChatService.suggest(travelId, currentUser, request);
                streamMessage(emitter, response.assistantMessage());
                sendEvent(emitter, "result", response);
                emitter.complete();
            } catch (BusinessException exception) {
                sendErrorEvent(emitter, exception.getErrorCode().getCode(), exception.getErrorCode().getMessage());
                emitter.complete();
            } catch (RuntimeException exception) {
                emitter.completeWithError(exception);
            }
        });

        return emitter;
    }

    private void streamMessage(SseEmitter emitter, String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        message.codePoints().forEach(codePoint -> {
            sendEvent(emitter, "message", new String(Character.toChars(codePoint)));
            sleep();
        });
    }

    private void sendErrorEvent(SseEmitter emitter, String code, String message) {
        sendEvent(emitter, "error", Map.of(
                "code", code,
                "message", message
        ));
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to send SSE event", exception);
        }
    }

    private void sleep() {
        try {
            Thread.sleep(MESSAGE_DELAY_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while streaming SSE message", exception);
        }
    }
}
