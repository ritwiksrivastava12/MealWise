package in.mealwise.api.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {
  private final SecretKey key;
  public JwtService(@Value("${mealwise.jwt.secret}") String secret) {
    if (secret == null || secret.length() < 32) throw new IllegalStateException("JWT_SECRET must be >=32 chars (use secret manager)");
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }
  public String accessToken(String userId) {
    return Jwts.builder().subject(userId)
      .issuedAt(new Date()).expiration(Date.from(Instant.now().plusSeconds(900)))
      .signWith(key).compact();
  }
  public String subject(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
  }
}
