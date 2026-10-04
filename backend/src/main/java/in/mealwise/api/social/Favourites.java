package in.mealwise.api.social;

import in.mealwise.api.config.JwtFilter;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.*;

public class Favourites {
  @Entity @Table(name = "favourites")
  @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @IdClass(FavKey.class)
  public static class Fav {
    @Id @Column(name = "user_id") private UUID userId;
    @Id @Column(name = "meal_id") private UUID mealId;
    @Column(name = "created_at") private Instant createdAt;
  }
  @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
  public static class FavKey implements Serializable { private UUID userId; private UUID mealId; }
  public interface Repo extends JpaRepository<Fav, FavKey> {
    List<Fav> findByUserId(UUID userId);
  }
  @RestController @RequestMapping("/api/v1/users/me/favourites")
  public static class Ctrl {
    private final Repo repo; public Ctrl(Repo r) { repo = r; }
    @GetMapping public List<Fav> list() { return repo.findByUserId(UUID.fromString(JwtFilter.userId())); }
    @PostMapping("/{mealId}") public Map<String, Object> add(@PathVariable UUID mealId) {
      var f = Fav.builder().userId(UUID.fromString(JwtFilter.userId())).mealId(mealId).createdAt(Instant.now()).build();
      repo.save(f); return Map.of("ok", true);
    }
    @DeleteMapping("/{mealId}") public Map<String, Object> remove(@PathVariable UUID mealId) {
      repo.delete(Fav.builder().userId(UUID.fromString(JwtFilter.userId())).mealId(mealId).build());
      return Map.of("ok", true);
    }
  }
}
