package in.mealwise.api.auth;

import in.mealwise.api.common.ApiException;
import in.mealwise.api.config.JwtService;
import in.mealwise.api.users.*;
import jakarta.validation.constraints.*;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  public record RegisterReq(String email, String phoneE164, String password, String displayName) {}
  public record LoginReq(@NotBlank String emailOrPhone, @NotBlank String password) {}
  public record RefreshReq(@NotBlank String refreshToken) {}

  private final UserRepository users; private final PasswordEncoder enc; private final JwtService jwt;
  // Production: persist sessions in `sessions` table + Redis. In-memory map here is process-local fallback shape.
  private final Map<String, UUID> refreshIndex = new ConcurrentHashMap<>();
  public AuthController(UserRepository u, PasswordEncoder e, JwtService j) { users = u; enc = e; jwt = j; }

  @PostMapping("/register")
  public Map<String, Object> register(@RequestBody RegisterReq r) {
    if ((r.email() == null || r.email().isBlank()) && (r.phoneE164() == null || r.phoneE164().isBlank()))
      throw ApiException.bad("IDENTITY_REQUIRED", "Email or phone is required.");
    if (r.password() != null && r.password().length() < 10)
      throw ApiException.bad("WEAK_PASSWORD", "Password must be at least 10 characters.");
    var u = User.builder().email(norm(r.email())).phoneE164(r.phoneE164())
      .passwordHash(r.password() == null ? null : enc.encode(r.password())).build();
    try { users.save(u); }
    catch (Exception e) { throw new ApiException("ALREADY_EXISTS", "Account already exists.", HttpStatus.CONFLICT); }
    return Map.of("userId", u.getId().toString());
  }

  @PostMapping("/login")
  public Map<String, Object> login(@RequestBody LoginReq r) {
    var u = r.emailOrPhone().contains("@")
      ? users.findByEmailIgnoreCase(r.emailOrPhone().toLowerCase())
      : users.findByPhoneE164(r.emailOrPhone());
    var user = u.orElseThrow(() -> new ApiException("INVALID_CREDENTIALS", "Invalid credentials.", HttpStatus.UNAUTHORIZED));
    if (user.getPasswordHash() == null || !enc.matches(r.password(), user.getPasswordHash()))
      throw new ApiException("INVALID_CREDENTIALS", "Invalid credentials.", HttpStatus.UNAUTHORIZED);
    return tokens(user.getId());
  }

  @PostMapping("/refresh")
  public Map<String, Object> refresh(@RequestBody RefreshReq r) {
    UUID uid = refreshIndex.remove(r.refreshToken());
    if (uid == null) throw new ApiException("INVALID_REFRESH", "Session expired. Log in again.", HttpStatus.UNAUTHORIZED);
    return tokens(uid); // rotation: old token single-use
  }

  @PostMapping("/logout")
  public Map<String, Object> logout(@RequestBody(required = false) RefreshReq r) {
    if (r != null && r.refreshToken() != null) refreshIndex.remove(r.refreshToken());
    return Map.of("ok", true);
  }

  private Map<String, Object> tokens(UUID uid) {
    byte[] b = new byte[32]; new SecureRandom().nextBytes(b);
    String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    refreshIndex.put(refresh, uid);
    return Map.of("accessToken", jwt.accessToken(uid.toString()), "refreshToken", refresh,
      "expiresIn", 900, "tokenType", "Bearer");
  }
  private static String norm(String e) { return e == null ? null : e.trim().toLowerCase(); }
}
