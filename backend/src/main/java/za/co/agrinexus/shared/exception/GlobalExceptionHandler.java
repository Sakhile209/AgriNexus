/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.HttpStatusCode
 *  org.springframework.http.ResponseEntity
 *  org.springframework.security.authentication.BadCredentialsException
 *  org.springframework.web.bind.MethodArgumentNotValidException
 *  org.springframework.web.bind.annotation.ExceptionHandler
 *  org.springframework.web.bind.annotation.RestControllerAdvice
 */
package za.co.agrinexus.shared.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import za.co.agrinexus.shared.exception.ApiError;
import za.co.agrinexus.shared.exception.ConflictException;
import za.co.agrinexus.shared.exception.ResourceNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value={MethodArgumentNotValidException.class})
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        LinkedHashMap<String, String> errors = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_FAILED", "Please correct the highlighted fields.", errors, Instant.now()));
    }

    @ExceptionHandler(value={ConflictException.class})
    ResponseEntity<ApiError> conflict(ConflictException exception) {
        return ResponseEntity.status((HttpStatusCode)HttpStatus.CONFLICT).body(ApiError.of("CONFLICT", exception.getMessage()));
    }

    @ExceptionHandler(value={ResourceNotFoundException.class})
    ResponseEntity<ApiError> notFound(ResourceNotFoundException exception) {
        return ResponseEntity.status((HttpStatusCode)HttpStatus.NOT_FOUND).body(ApiError.of("NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(value={BadCredentialsException.class})
    ResponseEntity<ApiError> badCredentials() {
        return ResponseEntity.status((HttpStatusCode)HttpStatus.UNAUTHORIZED).body(ApiError.of("INVALID_CREDENTIALS", "Email or password is incorrect."));
    }
}
