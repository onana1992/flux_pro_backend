package com.nanotech.flux_pro_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/auth/change-password",
            "/api/auth/logout",
            "/api/users/me");

    private static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.FRENCH, Locale.ENGLISH);

    private final MessageSource messageSource;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser user && user.isMustChangePassword()) {
            String path = request.getRequestURI();
            if (!ALLOWED_PATHS.contains(path)) {
                Locale locale = resolveLocale(request);
                String title = translate("error.title.mustChangePassword", "Password change required", locale);
                String detailMessage = translate(
                        "AUTH_MUST_CHANGE_PASSWORD", "You must change your password before continuing", locale);
                ProblemDetail problem = ProblemDetail.forStatus(403);
                problem.setTitle(title);
                problem.setDetail(detailMessage);
                problem.setProperty("code", "MUST_CHANGE_PASSWORD");
                response.setStatus(403);
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(
                        "{\"title\":\"" + escape(title) + "\",\"status\":403,\"detail\":\"" + escape(detailMessage)
                                + "\",\"code\":\"MUST_CHANGE_PASSWORD\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String translate(String code, String fallback, Locale locale) {
        try {
            return messageSource.getMessage(code, null, fallback, locale);
        } catch (NoSuchMessageException e) {
            return fallback;
        }
    }

    private Locale resolveLocale(HttpServletRequest request) {
        String header = request.getHeader("Accept-Language");
        if (header == null || header.isBlank()) {
            return Locale.FRENCH;
        }
        try {
            List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(header);
            Locale match = Locale.lookup(ranges, SUPPORTED_LOCALES);
            return match != null ? match : Locale.FRENCH;
        } catch (IllegalArgumentException e) {
            return Locale.FRENCH;
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
