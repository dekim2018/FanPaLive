package com.livebroadcast.common.security;

import java.time.Instant;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.livebroadcast.common.error.DomainException;

public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class)
                && AuthUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            var jwt = token.getToken();
            Object joined = jwt.getClaims().get(JwtClaims.JOINED_AT);
            Instant joinedAt = joined instanceof Number n ? Instant.ofEpochSecond(n.longValue()) : null;
            return new AuthUser(
                    Long.valueOf(jwt.getSubject()),
                    jwt.getClaimAsString(JwtClaims.NICKNAME),
                    jwt.getClaimAsString(JwtClaims.ROLE),
                    joinedAt
            );
        }
        LoginUser annotation = parameter.getParameterAnnotation(LoginUser.class);
        if (annotation != null && annotation.required()) {
            throw DomainException.unauthorized("UNAUTHORIZED", "로그인이 필요합니다.");
        }
        return null;
    }
}
