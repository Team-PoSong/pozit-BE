package com.pozit.pozitserver.global.auth.google;

import com.pozit.pozitserver.global.auth.apple.AppleClient;
import com.pozit.pozitserver.global.auth.apple.jwt.AppleTokenClaims;
import com.pozit.pozitserver.global.auth.dto.response.LoginTokenResponse;
import com.pozit.pozitserver.global.auth.kakao.KakaoClient;
import com.pozit.pozitserver.global.auth.service.AuthGoogleService;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.user.domain.Role;
import com.pozit.pozitserver.user.domain.SocialProvider;
import com.pozit.pozitserver.user.domain.User;
import com.pozit.pozitserver.user.dto.response.KakaoUserResponse;
import com.pozit.pozitserver.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "pozing.edit.worker.enabled=false",
        "pozing.thumbnail.worker.enabled=false",
        "pozing.edit.cleanup.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
class AuthGoogleIntegrationTest {

    private static final String GOOGLE_TOKEN = "test-google-id-token";
    private static final String GOOGLE_LOGIN_PATH = "/api/auth/google";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository users;
    @Autowired private AuthGoogleService googleAuth;

    @MockitoBean private GoogleIdTokenVerifier verifier;
    @MockitoBean private KakaoClient kakaoClient;
    @MockitoBean private AppleClient appleClient;
    @MockitoBean private StringRedisTemplate redis;

    private final Map<String, String> redisValues = new ConcurrentHashMap<>();

    @BeforeEach
    void simulateRedisSessionStorage() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(call -> redisValues.get(call.getArgument(0)));
        doAnswer(call -> {
            redisValues.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(values).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        when(redis.hasKey(anyString())).thenAnswer(call -> redisValues.containsKey(call.getArgument(0)));
        when(redis.delete(anyString())).thenAnswer(call -> redisValues.remove(call.getArgument(0)) != null);
        when(redis.delete(anyCollection())).thenAnswer(call -> {
            Collection<String> keys = call.getArgument(0);
            return keys.stream().filter(key -> redisValues.remove(key) != null).count();
        });
        doReturn(mock(Cursor.class)).when(redis).scan(any(ScanOptions.class));
    }

    @Test
    void registersAndThenReusesGoogleAccountWithoutLinkingAnotherProvider() throws Exception {
        String subject = stubGoogleSubject();
        User kakaoUser = users.saveAndFlush(User.builder()
                .provider(SocialProvider.KAKAO)
                .socialId(subject)
                .nickname("kakao-" + subject)
                .role(Role.USER)
                .build());

        JsonNode first = googleLogin(true);
        JsonNode second = googleLogin(false);
        long userId = first.path("userId").asLong();

        assertThat(userId).isNotEqualTo(kakaoUser.getId());
        assertThat(second.path("userId").asLong()).isEqualTo(userId);
        User googleUser = users.findById(userId).orElseThrow();
        assertThat(googleUser.getProvider()).isEqualTo(SocialProvider.GOOGLE);
        assertThat(googleUser.getSocialId()).isEqualTo(subject);
        assertThat(googleUser.getPushEnabled()).isTrue();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentRegistrationCreatesExactlyOneGoogleAccount() throws Exception {
        String subject = stubGoogleSubject();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<LoginTokenResponse> login = () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Concurrent login did not start");
                }
                return googleAuth.loginWithGoogle(GOOGLE_TOKEN);
            };
            var first = executor.submit(login);
            var second = executor.submit(login);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var firstResult = first.get(15, TimeUnit.SECONDS);
            var secondResult = second.get(15, TimeUnit.SECONDS);
            assertThat(firstResult.userId()).isEqualTo(secondResult.userId());
            assertThat(firstResult.isNewUser()).isNotEqualTo(secondResult.isNewUser());
            assertThat(users.findByProviderAndSocialId(SocialProvider.GOOGLE, subject)).isPresent();
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            users.findByProviderAndSocialId(SocialProvider.GOOGLE, subject).ifPresent(users::delete);
        }
    }

    @Test
    void rejectsAbsentBlankMalformedAndInvalidGoogleTokens() throws Exception {
        mvc.perform(post(GOOGLE_LOGIN_PATH).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400"));
        for (String body : new String[]{"{}", "{\"idToken\":\" \"}", "{"}) {
            postJson(GOOGLE_LOGIN_PATH, body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON400"));
        }
        verifyNoInteractions(verifier);

        when(verifier.verifyAndGetSubject(GOOGLE_TOKEN))
                .thenThrow(new BusinessException(ErrorCode.GOOGLE_INVALID_ID_TOKEN));
        postJson(GOOGLE_LOGIN_PATH, "{\"idToken\":\"" + GOOGLE_TOKEN + "\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GOOGLE401"));
    }

    @Test
    void googleSessionCanRefreshAndLogoutWithTheExistingEndpoints() throws Exception {
        stubGoogleSubject();
        JsonNode login = googleLogin(true);
        JsonNode refreshed = result(postJson("/api/auth/reissue",
                mapper.writeValueAsString(Map.of("refreshToken", login.path("refreshToken").asText())))
                .andExpect(status().isOk()));

        String accessToken = refreshed.path("accessToken").asText();
        String refreshToken = refreshed.path("refreshToken").asText();
        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isUnauthorized());
        postJson("/api/auth/reissue", mapper.writeValueAsString(Map.of("refreshToken", refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void googleMemberCanWithdrawWithoutAppleCredentialsAndLosesAccess() throws Exception {
        String subject = stubGoogleSubject();
        JsonNode login = googleLogin(true);
        long userId = login.path("userId").asLong();
        String accessToken = login.path("accessToken").asText();

        mvc.perform(delete("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk());
        assertThat(users.findById(userId).orElseThrow().isDeleted()).isTrue();
        assertThat(users.findByProviderAndSocialId(SocialProvider.GOOGLE, subject)).isEmpty();
        verify(appleClient, never()).revokeAuthorizationCode(any(), any());
        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isUnauthorized());
        postJson("/api/auth/reissue",
                mapper.writeValueAsString(Map.of("refreshToken", login.path("refreshToken").asText())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void kakaoAndAppleStillIssuePozitTokens() throws Exception {
        when(kakaoClient.requestUserInfo("kakao-token"))
                .thenReturn(new KakaoUserResponse(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE, null));
        when(appleClient.loginWithAppleIdentityToken(any()))
                .thenReturn(new AppleTokenClaims(UUID.randomUUID().toString(), null, false, false));

        postJson("/api/auth/kakao/native", "{\"accessToken\":\"kakao-token\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.result.refreshToken").isNotEmpty());
        postJson("/api/auth/apple", """
                {
                  "identityToken": "apple-token",
                  "authorizationCode": "apple-code",
                  "nonce": "test-nonce",
                  "platform": "IOS"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.result.refreshToken").isNotEmpty());
    }

    private String stubGoogleSubject() {
        String subject = UUID.randomUUID().toString();
        when(verifier.verifyAndGetSubject(GOOGLE_TOKEN)).thenReturn(subject);
        return subject;
    }

    private JsonNode googleLogin(boolean newUser) throws Exception {
        return result(postJson(GOOGLE_LOGIN_PATH, mapper.writeValueAsString(Map.of("idToken", GOOGLE_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.result.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.result.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.result.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.result.isNewUser").value(newUser)));
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode result(ResultActions call) throws Exception {
        return mapper.readTree(call.andReturn().getResponse().getContentAsString()).path("result");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
