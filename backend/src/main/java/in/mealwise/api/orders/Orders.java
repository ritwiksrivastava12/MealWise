package in.mealwise.api.orders;

import in.mealwise.api.common.ApiException;
import in.mealwise.api.config.JwtFilter;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.*;

public class Orders {
  static final Map<String, Set<String>> FLOW = Map.ofEntries(
    Map.entry("CREATED", Set.of("QUOTE_PENDING", "CANCELLED", "FAILED")),
    Map.entry("QUOTE_PENDING", Set.of("CONFIRMED", "CANCELLED", "FAILED")),
    Map.entry("CONFIRMED", Set.of("PAYMENT_PENDING", "CANCELLED")),
    Map.entry("PAYMENT_PENDING", Set.of("PAID", "FAILED", "CANCELLED")),
    Map.entry("PAID", Set.of("ACCEPTED", "REFUND_PENDING", "CANCELLED")),
    Map.entry("ACCEPTED", Set.of("PREPARING", "CANCEL_REQUESTED")),
    Map.entry("PREPARING", Set.of("DISPATCHED", "CANCEL_REQUESTED")),
    Map.entry("DISPATCHED", Set.of("DELIVERED", "FAILED")),
    Map.entry("CANCEL_REQUESTED", Set.of("CANCELLED", "REFUNDED")),
    Map.entry("DELIVERED", Set.of()), Map.entry("CANCELLED", Set.of()),
    Map.entry("FAILED", Set.of()), Map.entry("REFUND_PENDING", Set.of("REFUNDED")),
    Map.entry("REFUNDED", Set.of()));

  @Entity @Table(name = "orders")
  @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
  public static class Order {
    @Id private UUID id; @Column(name = "user_id") private UUID userId;
    private String provider; private String state;
    @Column(name = "idempotency_key", unique = true) private String idempotencyKey;
    @Column(name = "handoff_url") private String handoffUrl;
    @Column(name = "state_history", columnDefinition = "jsonb") private String stateHistory = "[]";
    @Version private long version;
    @PrePersist void pre() { if (id == null) id = UUID.randomUUID(); if (state == null) state = "CREATED"; }
  }
  public interface Repo extends JpaRepository<Order, UUID> {
    Optional<Order> findByIdempotencyKey(String k);
    List<Order> findByUserIdOrderByIdDesc(UUID u);
  }
  @RestController @RequestMapping("/api/v1/orders")
  public static class Ctrl {
    private final Repo repo; public Ctrl(Repo r) { repo = r; }
    @PostMapping public Order create(@RequestBody Map<String, Object> b) {
      String key = (String) b.get("idempotencyKey");
      if (key == null || key.isBlank()) throw ApiException.bad("IDEMPOTENCY_REQUIRED", "idempotencyKey is required");
      var existing = repo.findByIdempotencyKey(key);
      if (existing.isPresent()) return existing.get();
      var o = Order.builder().userId(UUID.fromString(JwtFilter.userId()))
        .provider((String) b.getOrDefault("provider", "handoff"))
        .handoffUrl((String) b.get("handoffUrl")).idempotencyKey(key).build();
      return repo.save(o);
    }
    @GetMapping public List<Order> mine() { return repo.findByUserIdOrderByIdDesc(UUID.fromString(JwtFilter.userId())); }
    @GetMapping("/{id}") public Order one(@PathVariable UUID id) {
      var o = repo.findById(id).orElseThrow(() -> ApiException.notFound("Order not found"));
      if (!o.getUserId().toString().equals(JwtFilter.userId())) throw ApiException.forbidden("Not yours");
      return o;
    }
    @PostMapping("/{id}/transition") public Order transition(@PathVariable UUID id, @RequestBody Map<String, String> b) {
      var o = one(id);
      String to = b.get("to");
      if (!FLOW.getOrDefault(o.getState(), Set.of()).contains(to))
        throw ApiException.bad("ILLEGAL_TRANSITION", "Cannot move " + o.getState() + " → " + to);
      o.setState(to);
      o.setStateHistory(o.getStateHistory() + ";{\"to\":\"" + to + "\",\"at\":\"" + Instant.now() + "\"}");
      return repo.save(o);
    }
  }
}
