package com.pozit.pozitserver.global.auth.service;

import com.pozit.pozitserver.global.auth.annotation.CurrentUser;
import com.pozit.pozitserver.global.auth.jwt.TokenType;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.user.domain.User;
import com.pozit.pozitserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver
        implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;
    private final AuthTokenService authTokenService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        CurrentUser currentUserAnnotation = parameter.getParameterAnnotation(CurrentUser.class);
        boolean required = currentUserAnnotation == null || currentUserAnnotation.required();

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            if (!required) {
                return null;
            }
            throw new BusinessException(ErrorCode.COMMON401);
        }

        String tokenType = jwt.getClaimAsString("type");
        if (!TokenType.ACCESS.name().equals(tokenType)) {
            throw new BusinessException(ErrorCode.COMMON401);
        }
        authTokenService.validateAccessTokenNotBlacklisted(jwt);

        Long userId;
        try {
            userId = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.COMMON401);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON401));
        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMON401);
        }
        return user;
    }
}
