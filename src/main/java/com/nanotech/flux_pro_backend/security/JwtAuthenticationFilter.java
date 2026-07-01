package com.nanotech.flux_pro_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            var claims = jwtTokenProvider.parseToken(token);
            if (!jwtTokenProvider.isAccessToken(claims)) {
                filterChain.doFilter(request, response);
                return;
            }
            String email = claims.get("email", String.class);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityUser user = (SecurityUser) userDetailsService.loadUserByUsername(email);
                if (!user.isEnabled()) {
                    filterChain.doFilter(request, response);
                    return;
                }
                if (!user.isAccountNonLocked()) {
                    filterChain.doFilter(request, response);
                    return;
                }
                var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception ignored) {
            // Invalid token — leave unauthenticated
        }
        filterChain.doFilter(request, response);
    }
}
