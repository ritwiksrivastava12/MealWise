package in.mealwise.api.meals;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity @Table(name = "meals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Meal {
  @Id private UUID id;
  private String slug; private String name;
  @Column(columnDefinition = "text") private String description;
  private String diet; // vegetarian|non_veg|eggetarian
  private String categories; // comma list (simple; normalized tags table in V2)
  private String difficulty;
  @Column(name = "prep_minutes") private int prepMinutes;
  @Column(name = "cook_minutes") private int cookMinutes;
  @Column(name = "servings_default") private int servingsDefault;
  @Column(name = "image_url") private String imageUrl;
  private String tags;
  @Column(name = "is_active") @Builder.Default private boolean active = true;
  @PrePersist void pre() { if (id == null) id = UUID.randomUUID(); }
}
