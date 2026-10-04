package in.mealwise.api.meals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnginesTest {
  @Test void scaleTwoToFour() {
    assertEquals(new BigDecimal("200.000"), Engines.scale(new BigDecimal("50.000"), 4));
    assertEquals("200", Engines.display(new BigDecimal("200.000"), "g"));
  }
  @Test void nutritionMath() {
    var ing = Ingredient.builder().id(UUID.randomUUID()).name("Paneer")
      .baseUnit("g").kcalPer100(265).proteinPer100(18).carbsPer100(3.6).fatPer100(20).fibrePer100(0).build();
    Map<String, Double> n = Engines.nutrition(List.of(new Engines.Row(ing, 100.0, "g")));
    assertEquals(265.0, n.get("kcal"));
    assertEquals(18.0, n.get("proteinG"));
  }
  @Test void unitsConvert() {
    assertEquals(1000.0, Engines.toBase(1, "kg"));
    assertEquals(5.0, Engines.toBase(1, "tsp"));
  }
}
