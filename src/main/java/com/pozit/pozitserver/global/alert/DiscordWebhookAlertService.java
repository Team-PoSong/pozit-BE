package com.pozit.pozitserver.global.alert;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscordWebhookAlertService {

    private static final int DISCORD_CONTENT_LIMIT = 2000;
    private static final int STACK_TRACE_LIMIT = 900;

    private final DiscordWebhookProperties properties;
    private final WebClient.Builder webClientBuilder;
    private final Environment environment;

    public void sendServerErrorAlert(Throwable exception, HttpServletRequest request, int status) {
        if (!properties.isAvailable()) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", properties.getUsername());
        payload.put("content", buildContent(exception, request, status));

        webClientBuilder
                .clone()
                .build()
                .post()
                .uri(properties.getUrl())
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(error -> {
                    log.warn("Failed to send Discord webhook alert", error);
                    return Mono.empty();
                })
                .subscribe();
    }

    private String buildContent(Throwable exception, HttpServletRequest request, int status) {
        String requestPath = request.getRequestURI();
        if (request.getQueryString() != null) {
            requestPath += "?" + request.getQueryString();
        }

        String content = """
                **[POZIT Server Error]**
                
                **Request**
                ```http
                %s %s -> %d
                ```
                **Environment**
                `profile` %s
                `time` %s
                
                **Exception**
                `type` %s
                `message` %s
                
                **Stack Trace**
                ```text
                %s
                ```
                """.formatted(
                request.getMethod(),
                escapeCodeBlock(requestPath),
                status,
                inlineCode(activeProfiles()),
                inlineCode(OffsetDateTime.now(ZoneId.of("Asia/Seoul")).toString()),
                inlineCode(exception.getClass().getName()),
                inlineCode(safeMessage(exception)),
                escapeCodeBlock(abbreviate(stackTrace(exception), STACK_TRACE_LIMIT))
        );

        return abbreviate(content, DISCORD_CONTENT_LIMIT);
    }

    private String activeProfiles() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return "default";
        }

        return String.join(",", Arrays.asList(activeProfiles));
    }

    private String safeMessage(Throwable exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return "(empty)";
        }

        return exception.getMessage();
    }

    private String stackTrace(Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private String abbreviate(String value, int limit) {
        if (value.length() <= limit) {
            return value;
        }

        return value.substring(0, limit - 3) + "...";
    }

    private String inlineCode(String value) {
        return "`" + value.replace("`", "'") + "`";
    }

    private String escapeCodeBlock(String value) {
        return value.replace("```", "'''");
    }
}
