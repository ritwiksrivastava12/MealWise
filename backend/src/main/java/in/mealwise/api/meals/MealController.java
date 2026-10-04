package in.mealwise.api.meals;

import in.mealwise.api.common.ApiException;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/meals")
public class MealController {
  private final MealRepository meals;
  private final MealIngredientRepository links;
  private final IngredientRepository ingredients;
  public MealController(MealRepository m, MealIngredientRepository l, IngredientRepository i) { meals = m; links = l; ingredients = i; }

  @GetMapping("/search")
  public Map<String, Object> search(@RequestParam(required = false) String q,
      @RequestParam(required = false) String diet,
      @RequestParam(required = false) Integer maxMinutes,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    var p = meals.search(q, diet, maxMinutes, PageRequest.of(Math.min(page, 50), Math.min(size, 50)));
    return Map.of("content", p.getContent(), "page", p.getNumber(), "size", p.getSize(),
      "totalElements", p.getTotalElements(), "totalPages", p.getTotalPages());
  }

  @GetMapping("/{id}")
  public Map<String, Object> detail(@PathVariable UUID id, @RequestParam(defaultValue = "2") int servings) {
    var m = meals.findById(id).orElseThrow(() -> ApiException.notFound("Meal not found"));
    var rows = links.findByMealId(id);
    var scaled = rows.stream().map(r -> {
      var ing = ingredients.findById(r.getIngredientId()).orElseThrow();
      BigDecimal qty = Engines.scale(r.getQtyPerServing(), servings);
      return Map.of("ingredientId", ing.getId().toString(), "name", ing.getName(),
        "qty", qty, "qtyDisplay", Engines.display(qty, r.getUnit()), "unit", r.getUnit(), "note", r.getNote() == null ? "" : r.getNote());
    }).toList();
    var nutri = Engines.nutrition(rows.stream().map(r -> {
      var ing = ingredients.findById(r.getIngredientId()).orElseThrow();
      double base = Engines.toBase(Engines.scale(r.getQtyPerServing(), servings).doubleValue(), r.getUnit());
      return new Engines.Row(ing, base, ing.getBaseUnit());
    }).toList());
    return Map.of("meal", m, "servings", servings, "ingredients", scaled,
      "nutrition", nutri, "nutritionLabel", "estimated");
  }

  @PostMapping("/scale")
  public Map<String, Object> scale(@RequestBody Map<String, Object> b) {
    UUID mealId = UUID.fromString((String) b.get("mealId"));
    int servings = ((Number) b.get("servings")).intValue();
    if (servings < 1 || servings > 20) throw ApiException.bad("BAD_SERVINGS", "Servings must be 1..20");
    return detail(mealId, servings);
  }

  @PostMapping("/nutrition/calculate")
  public Map<String, Object> nutrition(@RequestBody Map<String, Object> b) {
    UUID mealId = UUID.fromString((String) b.get("mealId"));
    int servings = ((Number) b.getOrDefault("servings", 2)).intValue();
    var d = detail(mealId, servings);
    return Map.of("nutrition", d.get("nutrition"), "label", "estimated",
      "disclaimer", "General wellness estimate, not medical advice.");
  }
}
