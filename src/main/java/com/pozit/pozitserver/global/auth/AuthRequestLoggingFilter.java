package com.pozit.pozitserver.global.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Records auth traffic before Spring Security without logging credentials. */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthRequestLoggingFilter extends OncePerRequestFilter {

    private static final Set<String> AUTH_PATHS = Set.of(
            "/api/auth/kakao", "/api/auth/kakao/callback", "/api/auth/kakao/native",
            "/api/auth/apple", "/api/auth/apple/callback", "/api/auth/reissue",
            "/api/auth/logout"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !AUTH_PATHS.contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        log.info("Auth request received: method={} path={}",
                request.getMethod(), request.getServletPath());
        boolean failed = false;
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            failed = true;
            throw exception;
        } finally {
            log.info("Auth request completed: method={} path={} status={} failed={} durationMs={}",
                    request.getMethod(), request.getServletPath(), response.getStatus(), failed,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
        }
    }
}
