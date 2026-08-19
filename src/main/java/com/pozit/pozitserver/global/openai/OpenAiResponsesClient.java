package com.pozit.pozitserver.global.openai;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiResponsesClient {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final WebClient openAiWebClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    public <T> T createStructuredResponse(
            String systemPrompt,
            String userPrompt,
            String schemaName,
            Map<String, Object> schema,
            Class<T> responseType
    ) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new BusinessException(ErrorCode.OPENAI_API_KEY_MISSING);
        }

        Map<String, Object> request = Map.of(
                "model", properties.model(),
                "input", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "text", Map.of(
                        "format", Map.of(
                                "type", "json_schema",
                                "name", schemaName,
                                "strict", true,
                                "schema", schema
                        )
                )
        );

        String responseBody;
        try {
            responseBody = openAiWebClient.post()
                    .uri("/responses")
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> {
                                log.warn("OpenAI API request failed. status={}, body={}",
                                        response.statusCode().value(),
                                        abbreviate(body, 1000)
                                );
                                return Mono.error(new BusinessException(ErrorCode.OPENAI_REQUEST_FAILED));
                            }))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                    .block();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("OpenAI API request failed before receiving a response. model={}", properties.model(), exception);
            throw new BusinessException(ErrorCode.OPENAI_REQUEST_FAILED);
        }

        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, MAP_TYPE);
            String outputText = extractOutputText(response);
            return objectMapper.readValue(outputText, responseType);
        } catch (RuntimeException exception) {
            log.warn("OpenAI API response parse failed. body={}", abbreviate(responseBody, 1000), exception);
            throw new BusinessException(ErrorCode.OPENAI_RESPONSE_PARSE_FAILED);
        }
    }

    private String extractOutputText(Map<String, Object> response) {
        Object directOutputText = response.get("output_text");
        if (directOutputText instanceof String outputText && !outputText.isBlank()) {
            return outputText;
        }

        Object output = response.get("output");
        if (!(output instanceof List<?> outputItems)) {
            throw new BusinessException(ErrorCode.OPENAI_RESPONSE_PARSE_FAILED);
        }

        for (Object outputItem : outputItems) {
            if (!(outputItem instanceof Map<?, ?> item)) {
                continue;
            }

            Object content = item.get("content");
            if (!(content instanceof List<?> contents)) {
                continue;
            }

            for (Object contentItem : contents) {
                if (!(contentItem instanceof Map<?, ?> contentMap)) {
                    continue;
                }

                Object text = contentMap.get("text");
                if (text instanceof String outputText && !outputText.isBlank()) {
                    return outputText;
                }
            }
        }

        throw new BusinessException(ErrorCode.OPENAI_RESPONSE_PARSE_FAILED);
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
