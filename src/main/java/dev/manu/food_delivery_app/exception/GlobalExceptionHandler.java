package dev.manu.food_delivery_app.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleBadCredentialsException
        (BadCredentialsException ex, HttpServletRequest request) {
        return ResponseEntity.status(401).body(body(401, "Invalid email or password", request, null));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleStatus(
            ResponseStatusException exception, HttpServletRequest request) {
        int statusCode = exception.getStatusCode().value();
        String message = exception.getReason() != null ? exception.getReason() : "Unexpected error";
        return ResponseEntity.status(statusCode).body(body(statusCode, message, request, null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(
                        fe.getField(),
                        fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value"
                )).toList();
        return ResponseEntity.badRequest().body(body(400, "Validation failed", request, fieldErrors));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadInput(HttpServletRequest request, Exception exception) {
        return ResponseEntity.badRequest().body(body(400, "Malformed request body", request, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(HttpServletRequest request, Exception exception) {
        if (exception instanceof AccessDeniedException || exception instanceof AuthorizationDeniedException) {
            return ResponseEntity.status(403).body(body(403, "Access denied", request, null));
        }
        if (exception instanceof NoHandlerFoundException || exception instanceof NoResourceFoundException) {
            return ResponseEntity.status(404).body(body(404, "Endpoint not found", request, null));
        }
        if (exception instanceof HttpRequestMethodNotSupportedException) {
            return ResponseEntity.status(405).body(body(405, "Requested method not supported", request, null));
        }
        if(exception instanceof HttpMediaTypeNotSupportedException){
            return ResponseEntity.status(415).body(body(415, "Unsupported content type", request, null));
        }
        log.error("Unhandled exception on {} {}" , request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.status(500).body(body(500, "Internal server error", request, null));
    }

    private ErrorResponse body(
            int status, String message, HttpServletRequest request, List<ErrorResponse.FieldError> fieldErrors
    ) {
        return new ErrorResponse(
                Instant.now(),
                status,
                HttpStatus.resolve(status).getReasonPhrase(),
                message,
                request.getRequestURI(),
                fieldErrors
        );
    }
}
