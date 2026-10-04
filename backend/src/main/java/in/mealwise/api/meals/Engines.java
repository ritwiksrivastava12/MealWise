package in.mealwise.api.meals;

import java.math.*;
import java.util.*;

// Deterministic engines — pure Java, unit-tested, never delegated to the LLM.
public final class Engines {
  private Engines() {}
  static final Map<String, Double> TO_BASE = Map.of(
    "g", 1.0, "kg", 1000.0, "ml", 1.0, "l", 1000.0, "pc", 1.0,
    "pcs", 1.0, "tsp", 5.0, "tbsp", 15.0, "cup", 240.0);

  /** Scale per-serving qty to target servings. Internal precision kept; display rounded kitchen-friendly. */
  public static BigDecimal scale(BigDecimal perServing, int servings) {
    return perServing.multiply(BigDecimal.valueOf(servings)).setScale(3, RoundingMode.HALF_UP);
  }
  public static String display(BigDecimal qty, String unit) {
    double v = qty.doubleValue();
    // Kitchen-friendly: g/ml round to nearest 5 above 50, 1 decimal below; tsp/tbsp to nearest 0.25.
    return switch (unit) {
      case "g", "ml" -> v >= 50 ? String.valueOf(Math.round(v / 5) * 5) : String.format(java.util.Locale.ROOT, "%.0f", v);
      case "tsp", "tbsp", "cup" -> String.format(java.util.Locale.ROOT, "%.2f", Math.round(v * 4) / 4.0);
      default -> qty.stripTrailingZeros().toPlainString();
    };
  }
  public static double toBase(double qty, String unit) {
    return qty * TO_BASE.getOrDefault(unit.toLowerCase(java.util.Locale.ROOT), 1.0);
  }
  /** Nutrition from per-100g ingredient data. Returns ESTIMATED values (labelled by callers). */
  public static Map<String, Double> nutrition(List<Row> rows) {
    double kcal = 0, p = 0, c = 0, f = 0, fi = 0;
    for (var r : rows) {
      double grams = approxGrams(r.qtyBase(), r.baseUnit());
      double k = grams / 100.0;
      kcal += r.ingredient().getKcalPer100() * k; p += r.ingredient().getProteinPer100() * k;
      c += r.ingredient().getCarbsPer100() * k; f += r.ingredient().getFatPer100() * k;
      fi += r.ingredient().getFibrePer100() * k;
    }
    return Map.of("kcal", r1(kcal), "proteinG", r1(p), "carbsG", r1(c), "fatG", r1(f), "fibreG", r1(fi));
  }
  private static double approxGrams(double qtyBase, String baseUnit) {
    // pc/ml→g approximations kept conservative; callers label results estimated.
    return switch (baseUnit) { case "ml" -> qtyBase; case "pc" -> qtyBase * 50.0; default -> qtyBase; };
  }
  private static double r1(double v) { return Math.round(v * 10) / 10.0; }
  public record Row(Ingredient ingredient, double qtyBase, String baseUnit) {}
}
