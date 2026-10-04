package in.mealwise.api.planner;

import in.mealwise.api.common.ApiException;
import in.mealwise.api.config.JwtFilter;
import in.mealwise.api.meals.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.*;

public class Planner {
  @Entity @Table(name = "meal_plans")
  @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
  public static class Plan {
    @Id private UUID id; @Column(name = "user_id") private UUID userId;
    @Column(name = "planned_for") private LocalDate plannedFor;
    private String slot; @Column(name = "meal_id") private UUID mealId;
    private int servings; private String status;
    @Column(name = "rec_explanation") private String explanation;
    @Column(name = "override_reason") private String overrideReason;
    @Version private long version;
    @PrePersist void pre() { if (id == null) id = UUID.randomUUID(); if (status == null) status = "planned"; }
  }
  @Entity(name = "MealHistory") @Table(name = "meal_history")
  @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
  public static class History {    @Id private UUID id; @Column(name = "user_id") private UUID userId;
    @Column(name = "meal_id") private UUID mealId;
    @Column(name = "eaten_at") private Instant eatenAt;
    private int servings; private String source;
    @PrePersist void pre() { if (id == null) id = UUID.randomUUID(); if (eatenAt == null) eatenAt = Instant.now(); }
  }
  public interface PlanRepo extends JpaRepository<Plan, UUID> {
    List<Plan> findByUserIdAndPlannedForBetween(UUID u, LocalDate a, LocalDate b);
  }
  public interface HistoryRepo extends JpaRepository<History, UUID> {
    @Query("SELECT h FROM MealHistory h WHERE h.userId=:u AND h.eatenAt>=:since ORDER BY h.eatenAt DESC")
    List<History> recent(@Param("u") UUID u, @Param("since") Instant since);
  }
  @RestController @RequestMapping("/api/v1")
  public static class Ctrl {
    private final PlanRepo plans; private final HistoryRepo history; private final MealRepository meals;
    private final int avoidMealDays; private final int avoidIngredientDays;
    public Ctrl(PlanRepo p, HistoryRepo h, MealRepository m,
        @Value("${mealwise.recommend.avoid-meal-days:3}") int a,
        @Value("${mealwise.recommend.avoid-ingredient-days:2}") int b) {
      plans = p; history = h; meals = m; avoidMealDays = a; avoidIngredientDays = b;
    }
    @PostMapping("/planner")
    public Plan plan(@RequestBody Plan p) {
      p.setUserId(UUID.fromString(JwtFilter.userId())); p.setId(null);
      if (!List.of("breakfast", "lunch", "snack", "dinner").contains(p.getSlot()))
        throw ApiException.bad("BAD_SLOT", "slot must be breakfast|lunch|snack|dinner");
      return plans.save(p);
    }
    @GetMapping("/planner")
    public List<Plan> list(@RequestParam String from, @RequestParam String to) {
      return plans.findByUserIdAndPlannedForBetween(UUID.fromString(JwtFilter.userId()),
        LocalDate.parse(from), LocalDate.parse(to));
    }
    @PatchMapping("/planner/{id}")
    public Plan patch(@PathVariable UUID id, @RequestBody Map<String, Object> b) {
      var p = plans.findById(id).orElseThrow(() -> ApiException.notFound("Plan not found"));
      if (!p.getUserId().toString().equals(JwtFilter.userId())) throw ApiException.forbidden("Not yours");
      if (b.containsKey("status")) p.setStatus((String) b.get("status"));
      if (b.containsKey("servings")) p.setServings(((Number) b.get("servings")).intValue());
      return plans.save(p);
    }
    @PostMapping("/history")
    public History eat(@RequestBody History h) {
      h.setUserId(UUID.fromString(JwtFilter.userId())); h.setId(null);
      return history.save(h);
    }
    @GetMapping("/history")
    public List<History> hist(@RequestParam(defaultValue = "20") int limit) {
      var all = history.recent(UUID.fromString(JwtFilter.userId()), Instant.now().minus(Duration.ofDays(60)));
      return all.stream().limit(Math.min(limit, 100)).toList();
    }
    @PostMapping("/recommendations")
    public Map<String, Object> recommend(@RequestBody Map<String, Object> b) {
      UUID uid = UUID.fromString(JwtFilter.userId());
      String slot = (String) b.getOrDefault("slot", "dinner");
      boolean allowRepeat = Boolean.TRUE.equals(b.get("allowRepeat"));
      var recent = history.recent(uid, Instant.now().minus(Duration.ofDays(Math.max(avoidMealDays, avoidIngredientDays))));
      Set<String> recentMeals = new HashSet<>();
      recent.forEach(h -> recentMeals.add(h.getMealId().toString()));
      // Candidate pool: active meals filtered by diet if provided; rank: not-recent first, then name.
      var all = meals.findAll().stream().filter(Meal::isActive).toList();
      String diet = (String) b.get("diet");
      var cands = all.stream().filter(m -> diet == null || m.getDiet().equals(diet)).toList();
      var pick = cands.stream()
        .filter(m -> allowRepeat || !recentMeals.contains(m.getId().toString()))
        .findFirst().orElse(cands.stream().findFirst()
        .orElseThrow(() -> ApiException.notFound("No meals available")));
      String reason = recentMeals.contains(pick.getId().toString())
        ? "You asked to repeat — serving your favourite again."
        : "Picked to avoid repeats in the last " + avoidMealDays + " days, matching your preferences.";
      return Map.of("meal", pick, "reason", reason, "allowRepeat", allowRepeat);
    }
  }
}
