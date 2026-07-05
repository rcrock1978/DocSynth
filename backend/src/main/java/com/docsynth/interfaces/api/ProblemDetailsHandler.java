package com.docsynth.interfaces.api;

import com.docsynth.infrastructure.security.TenantResolutionException;
import com.docsynth.infrastructure.security.RbacDeniedException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.UUID;

/**
 * Global exception handler returning RFC 9457 Problem Details.
 *
 * Every response includes a traceId extension field for cross-service
 * correlation (FR-013). Tenant-resolution and RBAC denials fail closed
 * with 401/403; validation errors return 422; unexpected errors return 500
 * with a generic message and a stable error_id for log lookup.
 */
@RestControllerAdvice
public class ProblemDetailsHandler {

    @ExceptionHandler(TenantResolutionException.class)
    public ResponseEntity<ProblemDetail> handleTenant(TenantResolutionException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Tenant resolution failed", ex.getMessage());
    }

    @ExceptionHandler(RbacDeniedException.class)
    public ResponseEntity<ProblemDetail> handleRbac(RbacDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Insufficient role", ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleValidation(ConstraintViolationException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArg(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAny(Exception ex) {
        String errorId = UUID.randomUUID().toString();
        // Log the errorId so the operator can correlate; do not echo the exception
        // message in the response (potential information leak).
        org.slf4j.LoggerFactory.getLogger(ProblemDetailsHandler.class)
            .error("Unhandled exception id={}", errorId, ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
            "An unexpected error occurred. Reference id: " + errorId);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create("https://docs.docsynth/errors/" + status.value()));
        String traceId = org.slf4j.MDC.get("traceId");
        if (traceId != null) {
            pd.setProperty("traceId", traceId);
        }
        return ResponseEntity.status(status).body(pd);
    }
}
