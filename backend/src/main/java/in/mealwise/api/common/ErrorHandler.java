package in.mealwise.api.common;

import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ErrorHandler {
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String, Object>> api(ApiException e) {
    return ResponseEntity.status(e.status()).body(Map.of(
      "code", e.code(), "message", e.getMessage(),
      "correlationId", CorrelationFilter.current()));
  }
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException e) {
    var fields = e.getBindingResult().getFieldErrors().stream()
      .collect(java.util.stream.Collectors.toMap(f -> f.getField(), f -> f.getDefaultMessage(), (a, b) -> a));
    return ResponseEntity.badRequest().body(Map.of(
      "code", "VALIDATION_FAILED", "message", "Invalid input",
      "correlationId", CorrelationFilter.current(), "fieldErrors", fields));
  }
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> unknown(Exception e) {
    // Never leak internals (no stack/SQL). Log full error server-side via framework logger.
    org.slf4j.LoggerFactory.getLogger(ErrorHandler.class).error("unhandled", e);
    return ResponseEntity.status(500).body(Map.of(
      "code", "INTERNAL_ERROR", "message", "Something went wrong. Try again.",
      "correlationId", CorrelationFilter.current()));
  }
}
