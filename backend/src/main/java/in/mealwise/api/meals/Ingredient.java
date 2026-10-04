package in.mealwise.api.meals;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity @Table(name = "ingredients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ingredient {
  @Id private UUID id;
  @Column(unique = true) private String name;
  private String category;
  @Column(name = "base_unit") private String baseUnit; // g|ml|pc
  @Column(name = "kcal_per_100g") private double kcalPer100;
  @Column(name = "protein_per_100g") private double proteinPer100;
  @Column(name = "carbs_per_100g") private double carbsPer100;
  @Column(name = "fat_per_100g") private double fatPer100;
  @Column(name = "fibre_per_100g") private double fibrePer100;
  @PrePersist void pre() { if (id == null) id = UUID.randomUUID(); }
}
