package in.mealwise.api.meals;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealIngredientRepository extends JpaRepository<MealIngredient, MealIngredient.Key> {
  List<MealIngredient> findByMealId(UUID mealId);
}
