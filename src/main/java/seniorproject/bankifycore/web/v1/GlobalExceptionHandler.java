package seniorproject.bankifycore.web.v1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

        @ExceptionHandler(Exception.class)
        public ResponseEntity<?> handleInternalError(Exception ex) {
                log.error("Unexpected error occurred", ex); // ✅ this WILL print stacktrace

                // DEV mode: return real message (temporarily) so you can debug fast
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(error("INTERNAL_SERVER_ERROR", ex.getMessage(),
                                                HttpStatus.INTERNAL_SERVER_ERROR));
        }

        private Map<String, Object> error(String code, String message, HttpStatus status) {
                return Map.of(
                                "timestamp", Instant.now().toString(),
                                "status", status.value(),
                                "error", code,
                                "message", message);
        }
}
