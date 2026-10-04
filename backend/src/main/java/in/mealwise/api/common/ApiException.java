package in.mealwise.api.common;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
  private final String code; private final HttpStatus status;
  public ApiException(String code, String message, HttpStatus status) { super(message); this.code = code; this.status = status; }
  public String code() { return code; }
  public HttpStatus status() { return status; }
  public static ApiException notFound(String m) { return new ApiException("NOT_FOUND", m, HttpStatus.NOT_FOUND); }
  public static ApiException bad(String code, String m) { return new ApiException(code, m, HttpStatus.BAD_REQUEST); }
  public static ApiException forbidden(String m) { return new ApiException("FORBIDDEN", m, HttpStatus.FORBIDDEN); }
  public static ApiException unavailable(String m) { return new ApiException("PROVIDER_NOT_ENABLED", m, HttpStatus.NOT_IMPLEMENTED); }
}
