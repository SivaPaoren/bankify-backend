package seniorproject.bankifycore.web.v1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<?> handleBadRequest(IllegalArgumentException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(error("BAD_REQUEST", ex.getMessage(), HttpStatus.BAD_REQUEST));
        }

        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<?> handleIllegalState(IllegalStateException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(error("BAD_REQUEST", ex.getMessage(), HttpStatus.BAD_REQUEST));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
                String message = ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .findFirst()
                        .map(err -> err.getField() + ": " + err.getDefaultMessage())
                        .orElse("Validation failed");

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(error("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST));
        }

        /**
         * ✅ IMPORTANT:
         * Spring's ResponseStatusException (used in your BootstrapController) should NOT be converted to 500.
         * This handler preserves the intended HTTP status (401/403/409/etc.).
         */
        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<?> handleResponseStatus(ResponseStatusException ex) {
                HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
                String message = (ex.getReason() != null && !ex.getReason().isBlank())
                        ? ex.getReason()
                        : status.getReasonPhrase();

                return ResponseEntity.status(status)
                        .body(error(status.name(), message, status));
        }

        /**
         * Catch-all (keep this LAST).
         * In production, avoid leaking internal exception messages to clients.
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<?> handleInternalError(Exception ex) {
                log.error("Unexpected error occurred", ex); // prints stacktrace

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(error(
                                "INTERNAL_SERVER_ERROR",
                                "Something went wrong", // ✅ safer for prod
                                HttpStatus.INTERNAL_SERVER_ERROR
                        ));
        }

        private Map<String, Object> error(String code, String message, HttpStatus status) {
                return Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", status.value(),
                        "error", code,
                        "message", message
                );
        }
}