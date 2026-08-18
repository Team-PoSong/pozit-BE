package com.pozit.pozitserver.global.alert;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordWebhookAlertServiceTest {

    @Test
    void buildPayloadContainsServerErrorContext() {
        DiscordWebhookProperties properties = new DiscordWebhookProperties();
        properties.setEnabled(true);
        properties.setUrl("https://discord.example/webhook");
        properties.setUsername("pozit error");

        Environment environment = new MockEnvironment().withProperty("spring.profiles.active", "test");
        DiscordWebhookAlertService alertService = new DiscordWebhookAlertService(
                properties,
                WebClient.builder(),
                environment
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/error");
        request.setQueryString("debug=true");
        request.addHeader("User-Agent", "JUnit");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");

        Map<String, Object> payload = alertService.buildPayload(
                new RuntimeException("Discord webhook test error"),
                request,
                500
        );

        assertThat(payload.get("username")).isEqualTo("pozit error");
        assertThat((String) payload.get("content"))
                .contains("**[POZIT] Server Error**")
                .contains("`status` `500`  `profile` `test`")
                .contains("GET /api/test/error?debug=true -> 500")
                .contains("`clientIp` `203.0.113.10`")
                .contains("`userAgent` `JUnit`")
                .contains("java.lang.RuntimeException")
                .contains("Discord webhook test error");
    }

    @Test
    void sendServerErrorAlertDoesNotCallWebhookWhenDisabled() {
        DiscordWebhookProperties properties = new DiscordWebhookProperties();
        properties.setEnabled(false);
        properties.setUrl("https://discord.example/webhook");

        AtomicBoolean called = new AtomicBoolean(false);
        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(request -> {
                    called.set(true);
                    return Mono.error(new AssertionError("Webhook should not be called"));
                });

        DiscordWebhookAlertService alertService = new DiscordWebhookAlertService(
                properties,
                webClientBuilder,
                new MockEnvironment()
        );

        alertService.sendServerErrorAlert(
                new RuntimeException("error"),
                new MockHttpServletRequest("GET", "/api/test/error"),
                500
        );

        assertThat(called).isFalse();
    }
}
