package in.mealwise.api.meals;

import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface MealRepository extends JpaRepository<Meal, UUID> {
  @Query(value = """
    SELECT * FROM meals m WHERE m.is_active
    AND (:diet IS NULL OR m.diet = :diet)
    AND (:q IS NULL OR m.name ILIKE CONCAT('%', CAST(:q AS text), '%')
      OR m.tags ILIKE CONCAT('%', CAST(:q AS text), '%')
      OR m.description ILIKE CONCAT('%', CAST(:q AS text), '%'))
    AND (:maxMinutes IS NULL OR (m.prep_minutes + m.cook_minutes) <= :maxMinutes)
    ORDER BY CASE WHEN CAST(:q AS text) IS NULL THEN 0
      WHEN m.name ILIKE CAST(:q AS text) THEN 0
      WHEN m.name ILIKE CONCAT(CAST(:q AS text), '%') THEN 1 ELSE 2 END, m.name
    """,
    countQuery = """
    SELECT COUNT(*) FROM meals m WHERE m.is_active
    AND (:diet IS NULL OR m.diet = :diet)
    AND (:q IS NULL OR m.name ILIKE CONCAT('%', CAST(:q AS text), '%')
      OR m.tags ILIKE CONCAT('%', CAST(:q AS text), '%')
      OR m.description ILIKE CONCAT('%', CAST(:q AS text), '%'))
    AND (:maxMinutes IS NULL OR (m.prep_minutes + m.cook_minutes) <= :maxMinutes)
    """, nativeQuery = true)
  Page<Meal> search(@Param("q") String q, @Param("diet") String diet,
    @Param("maxMinutes") Integer maxMinutes, Pageable p);
}
