package in.mealwise.api.kitchen;

import in.mealwise.api.common.ApiException;
import in.mealwise.api.config.JwtFilter;
import in.mealwise.api.meals.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import lombok.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.*;

public class Kitchen {
  @Entity @Table(name = "inventory_items")
  @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
  public static class Item {
    @Id private UUID id; @Column(name = "user_id") private UUID userId;
    @Column(name = "ingredient_id") private UUID ingredientId;
    private BigDecimal qty; private String unit; @Column(name = "qty_base") private double qtyBase;
    private String status; @Column(name = "expires_on") private LocalDate expiresOn;
    private String source; @Version private long version;
    @PrePersist void pre() { if (id == null) id = UUID.randomUUID(); refresh(); }
    void refresh() {
      qtyBase = Engines.toBase(qty == null ? 0 : qty.doubleValue(), unit == null ? "g" : unit);
      status = qtyBase <= 0 ? "exhausted"
        : expiresOn != null && expiresOn.isBefore(LocalDate.now()) ? "expired"
        : qtyBase < 100 ? "low" : "available";
    }
  }
  public interface ItemRepo extends JpaRepository<Item, UUID> { List<Item> findByUserId(UUID u); }
  @RestController @RequestMapping("/api/v1/inventory")
  public static class Ctrl {
    private final ItemRepo items; private final IngredientRepository ings;
    private final MealIngredientRepository links;
    public Ctrl(ItemRepo i, IngredientRepository g, MealIngredientRepository l) { items = i; ings = g; links = l; }
    @GetMapping public List<Map<String, Object>> list() {
      UUID u = UUID.fromString(JwtFilter.userId());
      return items.findByUserId(u).stream().map(it -> {
        String name = ings.findById(it.getIngredientId()).map(Ingredient::getName).orElse("?");
        return (Map<String, Object>) Map.of("item", it, "ingredientName", name);
      }).toList();
    }
    @PostMapping public Item add(@RequestBody Item it) {
      it.setUserId(UUID.fromString(JwtFilter.userId())); it.setId(null); it.refresh();
      return items.save(it);
    }
    @PatchMapping("/{id}") public Item patch(@PathVariable UUID id, @RequestBody Item b) {
      var it = items.findById(id).orElseThrow(() -> ApiException.notFound("Item not found"));
      if (!it.getUserId().toString().equals(JwtFilter.userId())) throw ApiException.forbidden("Not yours");
      if (b.getQty() != null) it.setQty(b.getQty());
      if (b.getUnit() != null) it.setUnit(b.getUnit());
      if (b.getExpiresOn() != null) it.setExpiresOn(b.getExpiresOn());
      it.refresh(); return items.save(it);
    }
    @PostMapping("/{id}/consume") public Item consume(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
      var it = items.findById(id).orElseThrow(() -> ApiException.notFound("Item not found"));
      double delta = ((Number) body.getOrDefault("qty", 0)).doubleValue();
      it.setQty(BigDecimal.valueOf(Math.max(0, it.getQty().doubleValue() - delta))); it.refresh();
      return items.save(it);
    }
  }

  /** Availability engine: POST /api/v1/meals/{mealId}/availability — partitions needs into available/low/missing. */
  @RestController @RequestMapping("/api/v1/meals")
  public static class AvailabilityCtrl {
    private final ItemRepo items; private final IngredientRepository ings;
    private final MealIngredientRepository links;
    public AvailabilityCtrl(ItemRepo i, IngredientRepository g, MealIngredientRepository l) { items = i; ings = g; links = l; }
    @PostMapping("/{mealId}/availability")
    public Map<String, Object> availability(@PathVariable UUID mealId, @RequestBody Map<String, Object> body) {
      int servings = ((Number) body.getOrDefault("servings", 2)).intValue();
      UUID u = UUID.fromString(JwtFilter.userId());
      var stock = new HashMap<UUID, Double>();
      items.findByUserId(u).forEach(it -> stock.merge(it.getIngredientId(), it.getQtyBase(), Double::sum));
      var avail = new ArrayList<>(); var low = new ArrayList<>(); var missing = new ArrayList<>();
      for (var r : links.findByMealId(mealId)) {
        var ing = ings.findById(r.getIngredientId()).orElseThrow();
        double need = Engines.toBase(Engines.scale(r.getQtyPerServing(), servings).doubleValue(), r.getUnit());
        double have = stock.getOrDefault(r.getIngredientId(), 0.0);
        var row = Map.of("ingredientId", ing.getId().toString(), "name", ing.getName(),
          "need", need, "have", have, "unit", ing.getBaseUnit());
        if (have >= need) avail.add(row);
        else if (have > 0) low.add(row);
        else missing.add(row);
      }
      return Map.of("available", avail, "low", low, "missing", missing, "servings", servings);
    }
  }
}
