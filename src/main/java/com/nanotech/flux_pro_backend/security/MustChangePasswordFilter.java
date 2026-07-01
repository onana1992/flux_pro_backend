package com.nanotech.flux_pro_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/auth/change-password",
            "/api/auth/logout",
            "/api/users/me");

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser user && user.isMustChangePassword()) {
            String path = request.getRequestURI();
            if (!ALLOWED_PATHS.contains(path)) {
                ProblemDetail problem = ProblemDetail.forStatus(403);
                problem.setTitle("Password change required");
                problem.setDetail("You must change your password before continuing");
                problem.setProperty("code", "MUST_CHANGE_PASSWORD");
                response.setStatus(403);
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(
                        "{\"title\":\"Password change required\",\"status\":403,\"detail\":\"You must change your password before continuing\",\"code\":\"MUST_CHANGE_PASSWORD\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
