package com.nanotech.flux_pro_backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Renvoie 401 (au lieu du 403 par défaut de Spring Security) pour toute requête non
 * authentifiée sur une route protégée — notamment un token JWT expiré, invalide ou absent,
 * silencieusement ignoré par {@link JwtAuthenticationFilter}, qui laisse alors passer un
 * principal anonyme jusqu'à {@code AuthorizationFilter}.
 *
 * <p>Sans ce point d'entrée explicite, Spring Security répond par défaut 403 via
 * {@code Http403ForbiddenEntryPoint}, ce qui empêche le frontend de déclencher son flux de
 * rafraîchissement de session : celui-ci ne réagit qu'au 401 (cf.
 * flux-pro-front/src/lib/api.ts, fonction {@code apiFetch}). C'est la cause du symptôme
 * observé après une longue période d'inactivité du navigateur (token d'accès expiré après
 * {@code fluxpro.jwt.access-expiration-ms}, alors qu'un refresh token valide existe encore).</p>
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.FRENCH, Locale.ENGLISH);

    private final MessageSource messageSource;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        Locale locale = resolveLocale(request);
        String title = translate("error.title.authFailed", "Authentification échouée", locale);
        String detailMessage = translate("AUTH_REQUIRED", "Authentication required", locale);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"title\":\"" + escape(title) + "\",\"status\":401,\"detail\":\"" + escape(detailMessage)
                        + "\",\"code\":\"AUTH_REQUIRED\"}");
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
