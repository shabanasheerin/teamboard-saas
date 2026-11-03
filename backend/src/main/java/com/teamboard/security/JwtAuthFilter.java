package com.teamboard.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthUserService authUserService;

    public JwtAuthFilter(JwtService jwtService, AuthUserService authUserService) {
        this.jwtService = jwtService;
        this.authUserService = authUserService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            AuthUser user = null;
            try {
                String email = jwtService.parse(header.substring(7)).getSubject();
                user = authUserService.findByEmail(email);
            } catch (JwtException | IllegalArgumentException ignored) {
                // invalid or expired token: the request continues unauthenticated and gets 401
            }

            if (user != null && user.active()) {
                // Temporary password: the only things allowed are changing it and reading "me"
                if (user.mustChangePassword() && !allowedWithTemporaryPassword(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"status\":403,\"message\":\"Please change your temporary password first\"}");
                    return;
                }
                var auth = new UsernamePasswordAuthenticationToken(
                        user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }

    private boolean allowedWithTemporaryPassword(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        return ("POST".equals(method) && "/api/users/me/password".equals(path))
                || ("GET".equals(method) && "/api/users/me".equals(path));
    }
}
