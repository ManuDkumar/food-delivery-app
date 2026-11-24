package dev.manu.restaurant_service.security;

import dev.manu.restaurant_service.exception.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

@Component
@AllArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper mapper;
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(401);
        response.setHeader("WWW-Authenticate", "Bearer");
        response.setContentType("application/json");

        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(), 401, "Unauthorized", "Authentication required", request.getRequestURI(), null);

        response.getWriter().write(mapper.writeValueAsString(errorResponse));
    }
}
