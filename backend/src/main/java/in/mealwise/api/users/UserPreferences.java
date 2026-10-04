package in.mealwise.api.users;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity @Table(name = "user_preferences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserPreferences {
  @Id @Column(name = "user_id") private UUID userId;
  @Builder.Default private String diet = "vegetarian"; // vegetarian|non_veg|eggetarian (extensible)
  @Column(name = "disliked_foods") private String dislikedCsv = "";
  private String allergies = "";
  @Column(name = "goals_json") private String goalsJson = "{}";
  @Column(name = "budget_tier") private String budgetTier = "value";
  @Column(name = "max_cook_minutes") private int maxCookMinutes = 30;
  @Column(name = "household_size") private int householdSize = 2;
  @Column(name = "cooking_skill") private String cookingSkill = "beginner";
  @Column(name = "updated_at") private Instant updatedAt;
}
