package com.a3solutions.fsm.security;

import com.a3solutions.fsm.auth.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.security
 * @project A3 Field Service Management Backend
 * Tailored for new JwtService with access/refresh tokens and typed JWT claims.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // 1) No Bearer header -> let Spring handle as anonymous
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);
        String email = null;

        try {
            // 2) Extract subject (email) from token
            email = jwtService.extractUsername(token);
        } catch (Exception e) {
            System.out.println("🚨 JWT ERROR in JwtAuthFilter.extractUsername: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            // Let Spring treat this as unauthenticated, entry point will send 401
            filterChain.doFilter(request, response);
            return;
        }

        // 3) Only allow ACCESS tokens, never REFRESH
        if (!jwtService.isAccessToken(token)) {
            System.out.println("🚫 JWT token is not an ACCESS token (type != access).");
            filterChain.doFilter(request, response);
            return;
        }

        // 4) Only authenticate if no current auth set
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var userDetails = userDetailsService.loadUserByUsername(email);

            boolean valid = jwtService.isTokenValid(token, userDetails);
            if (!valid) {
                System.out.println("🚫 JWT token is NOT valid for user " + email + " (expired or subject mismatch).");
            }

            if (valid) {
                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                System.out.println("✅ JWT authentication success for user " + email + " on " + request.getRequestURI());
            }
        }

        // 5) Continue chain
        filterChain.doFilter(request, response);
    }





//    @Override
//    protected void doFilterInternal(
//            @NonNull HttpServletRequest request,
//            @NonNull HttpServletResponse response,
//            @NonNull FilterChain filterChain
//    ) throws ServletException, IOException {
//
//        final String authHeader = request.getHeader("Authorization");
//
//        // ===== 1. No Authorization header =====
//        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        final String token = authHeader.substring(7);
//
//        // ===== 2. Safely extract username =====
//        final String email;
//        try {
//            email = jwtService.extractUsername(token);
//        } catch (Exception e) {
//            // Token invalid, malformed, or expired
//            System.out.println("JWT ERROR: " + e.getMessage());
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        // ===== 3. Only allow ACCESS tokens for authentication =====
//        if (!jwtService.isAccessToken(token)) {
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        // ===== 4. Avoid double-authentication =====
//        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//
//            var userDetails = userDetailsService.loadUserByUsername(email);
//
//            // Validate signature + expiration + subject match
//            if (jwtService.isTokenValid(token, userDetails)) {
//
//                var authToken = new UsernamePasswordAuthenticationToken(
//                        userDetails,
//                        null,
//                        userDetails.getAuthorities()
//                );
//
//                authToken.setDetails(
//                        new WebAuthenticationDetailsSource().buildDetails(request)
//                );
//
//                SecurityContextHolder.getContext().setAuthentication(authToken);
//            }
//        }
//
//        // ===== 5. Continue filter chain =====
//        filterChain.doFilter(request, response);
//    }
}
