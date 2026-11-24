package dev.manu.restaurant_service.security;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@AllArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filter(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(configurer ->
                configurer.requestMatchers("/ping", "/error", "/actuator/health", "/actuator/metrics/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/restaurants/**").permitAll()
                        .anyRequest().authenticated());

        http.csrf(csrf -> csrf.disable());
        http.exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint));
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
