package com.vycepay.common.exception;

import com.vycepay.common.choicebank.errors.ChoiceCustomerMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler. Returns consistent customer-safe error envelope.
 * Choice Bank upstream errors prefer Choice {@code msg} when present.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final VyceErrorCatalog catalog;

    public GlobalExceptionHandler(VyceErrorCatalog catalog) {
        this.catalog = catalog;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        log.warn("Business exception: {} - {}", e.getCode(), e.getMessage());
        String message = catalog.userMessage(e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getHttpStatus()).body(new ErrorResponse(
                e.getCode(), message, getRequestId(), null));
    }

    @ExceptionHandler(ChoiceBankUpstreamException.class)
    public ResponseEntity<ErrorResponse> handleChoiceUpstream(ChoiceBankUpstreamException e) {
        log.warn("Choice Bank upstream error: clientCode={} choiceCode={} path={} choiceMsg={} - {}",
                e.getCode(), e.getChoiceCode(), e.getChoicePath(), e.getChoiceMessage(), e.getMessage());
        // Choice-message-first: show Choice msg when present; Vyce catalog is fallback only.
        String message = ChoiceCustomerMessage.prefer(e.getChoiceMessage(), catalog.userMessage(e.getCode()));
        ErrorResponse body = new ErrorResponse(
                e.getCode(), message, getRequestId(), null);
        body.setChoiceCode(e.getChoiceCode());
        body.setChoiceRequestId(e.getChoiceRequestId());
        body.setChoicePath(e.getChoicePath());
        body.setRetryable(e.isRetryable());
        return ResponseEntity.status(e.getHttpStatus()).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException e) {
        log.warn("Bad request: {}", e.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "INVALID_REQUEST", catalog.userMessage("INVALID_REQUEST"), getRequestId(), null));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(IllegalStateException e) {
        log.warn("Conflict: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                "CONFLICT", catalog.userMessage("CONFLICT"), getRequestId(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String details = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", details);
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "VALIDATION_ERROR", catalog.userMessage("VALIDATION_ERROR"), getRequestId(), details));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(
                "FORBIDDEN", catalog.userMessage("FORBIDDEN"), getRequestId(), null));
    }

    /**
     * Unknown paths (often bots/load-balancer probes on public ports). Return 404 without ERROR noise.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e) {
        log.debug("No resource for path: {}", e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                "NOT_FOUND", catalog.userMessage("NOT_FOUND"), getRequestId(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
        log.error("Unexpected error requestId={}", getRequestId(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
                "INTERNAL_ERROR", catalog.userMessage("INTERNAL_ERROR"), getRequestId(), null));
    }

    private String getRequestId() {
        String id = MDC.get("requestId");
        if (id == null || id.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return id;
    }
}
