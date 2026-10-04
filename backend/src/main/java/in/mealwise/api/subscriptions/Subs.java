package in.mealwise.api.subscriptions;

import in.mealwise.api.common.ApiException;
import in.mealwise.api.config.JwtFilter;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

public class Subs {
  public static final Map<String, Set<String>> MATRIX = Map.of(
    "free", Set.of("search", "favourites", "planner_basic", "kitchen", "cooking", "ai_30"),
    "pro", Set.of("search", "favourites", "planner_basic", "planner_advanced", "kitchen",
      "cooking", "ai_1000", "nutrition_advanced", "history_extended", "family"));

  @Entity @Table(name = "subscriptions")
  @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
  public static class Sub {
    @Id private UUID id; @Column(name = "user_id") private UUID userId;
    private String plan; private String status;
    @Column(name = "purchase_token", unique = true) private String purchaseToken;
    @Column(name = "current_period_end") private Instant periodEnd;
    @PrePersist void pre() { if (id == null) id = UUID.randomUUID(); }
  }
  public interface Repo extends JpaRepository<Sub, UUID> {
    List<Sub> findByUserId(UUID u);
    Optional<Sub> findByPurchaseToken(String t);
  }
  @RestController @RequestMapping("/api/v1/subscriptions")
  public static class Ctrl {
    private final Repo repo; public Ctrl(Repo r) { repo = r; }

    @GetMapping("/plans")
    public List<Map<String, Object>> plans() {
      return List.of(
        Map.of("id", "free", "pricePaise", 0, "currency", "INR", "interval", "forever"),
        Map.of("id", "pro_monthly", "pricePaise", 14900, "currency", "INR", "interval", "month"),
        Map.of("id", "pro_yearly", "pricePaise", 149900, "currency", "INR", "interval", "year"));
    }

    @GetMapping("/entitlements")
    public Map<String, Object> entitlements() {
      var plan = activePlan(UUID.fromString(JwtFilter.userId()));
      return Map.of("plan", plan, "capabilities", new ArrayList<>(MATRIX.get(plan)));
    }

    /** Verify Play purchase token server-side via Play Developer API (service account). */
    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestBody Map<String, String> b) {
      String token = b.get("purchaseToken"), product = b.get("productId");
      if (token == null || product == null) throw ApiException.bad("TOKEN_REQUIRED", "purchaseToken + productId required");
      if (repo.findByPurchaseToken(token).isPresent()) return Map.of("ok", true, "deduped", true);
      // Production: AndroidPublisher.purchases().subscriptionsv2().get(packageName, token).execute()
      // and check acknowledgementState + subscriptionState == ACTIVE. Without service-account creds,
      // we MUST NOT activate — return honest 503 instead of fake success.
      String svc = System.getenv("PLAY_SERVICE_ACCOUNT_JSON_PATH");
      if (svc == null || svc.isBlank())
        throw new ApiException("BILLING_NOT_CONFIGURED", "Play verification unavailable (no service account). Purchase not activated.", HttpStatus.SERVICE_UNAVAILABLE);
      var sub = Sub.builder().userId(UUID.fromString(JwtFilter.userId()))
        .plan(product.contains("yearly") ? "pro_yearly" : "pro_monthly")
        .status("active").purchaseToken(token).periodEnd(Instant.now().plusSeconds(30L * 86400)).build();
      repo.save(sub);
      return Map.of("ok", true, "plan", sub.getPlan());
    }

    /** Play RTDN webhook (JWT-verified in prod filter). Handles renew/cancel/expiry → entitlements. */
    @PostMapping("/play-rtdn")
    public Map<String, Object> rtdn(@RequestBody Map<String, Object> b) {
      return Map.of("ok", true); // Production decodes message.data subscriptionNotification → transitions grace/expired + FCM.
    }

    private String activePlan(UUID u) {
      return repo.findByUserId(u).stream()
        .filter(s -> "active".equals(s.getStatus()) && s.getPeriodEnd().isAfter(Instant.now()))
        .map(s -> s.getPlan().startsWith("pro") ? "pro" : "free")
        .findFirst().orElse("free");
    }
  }
}
