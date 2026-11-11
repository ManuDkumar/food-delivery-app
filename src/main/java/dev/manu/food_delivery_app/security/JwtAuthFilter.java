package dev.manu.food_delivery_app.security;

import dev.manu.food_delivery_app.enums.UserStatus;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@AllArgsConstructor
@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JWTUtility jwtUtility;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        String authToken = null;
        String email = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authToken = authHeader.substring(7);
        }

        if (authToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {
                email = jwtUtility.extractUsername(authToken);
                AuthUserDetails authUser = (AuthUserDetails) customUserDetailsService.loadUserByUsername(email);

                if (authUser.getUser().getUserStatus() == UserStatus.ACTIVE) {
                    if (jwtUtility.validateToken(authUser, authToken)) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        authUser, null, authUser.getAuthorities()
                                );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (JwtException | UsernameNotFoundException e) {
                log.debug("Invalid token: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
