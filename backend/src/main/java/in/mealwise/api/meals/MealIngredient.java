package in.mealwise.api.meals;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.*;

@Entity @Table(name = "meal_ingredients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(MealIngredient.Key.class)
public class MealIngredient {
  @Id @Column(name = "meal_id") private UUID mealId;
  @Id @Column(name = "ingredient_id") private UUID ingredientId;
  @Column(name = "qty_per_serving", precision = 12, scale = 3) private BigDecimal qtyPerServing;
  private String unit; // g|ml|pc|tsp|tbsp|cup — normalized in service
  @Column(name = "preparation_note") private String note;
  @Column(name = "is_optional") private boolean optional;
  @Transient private String ingredientName; // joined in queries

  @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
  public static class Key implements java.io.Serializable { private UUID mealId; private UUID ingredientId; }
}
