package in.mealwise.api.shopping;

import in.mealwise.api.config.JwtFilter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.*;

public class Shopping {
  @Entity @Table(name = "shopping_lists")
  @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
  public static class SList {
    @Id private UUID id; @Column(name = "user_id") private UUID userId;
    private String title; private String status;
    @PrePersist void pre() { if (id == null) id = UUID.randomUUID(); if (status == null) status = "open"; }
  }
  @Entity @Table(name = "shopping_list_items")
  @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
  public static class SItem {
    @Id private UUID id; @Column(name = "list_id") private UUID listId;
    @Column(name = "ingredient_id") private UUID ingredientId;
    private BigDecimal qty; private String unit; private String status;
    @PrePersist void pre() { if (id == null) id = UUID.randomUUID(); if (status == null) status = "needed"; }
  }
  public interface ListRepo extends JpaRepository<SList, UUID> { List<SList> findByUserId(UUID u); }
  public interface ItemRepo extends JpaRepository<SItem, UUID> { List<SItem> findByListId(UUID l); }
  @RestController @RequestMapping("/api/v1/shopping-lists")
  public static class Ctrl {
    private final ListRepo lists; private final ItemRepo items;
    public Ctrl(ListRepo l, ItemRepo i) { lists = l; items = i; }
    @GetMapping public List<SList> all() { return lists.findByUserId(UUID.fromString(JwtFilter.userId())); }
    @PostMapping public SList create(@RequestBody Map<String, String> b) {
      return lists.save(SList.builder().userId(UUID.fromString(JwtFilter.userId()))
        .title(b.getOrDefault("title", "Weekly groceries")).build());
    }
    @PostMapping("/{id}/items") public SItem add(@PathVariable UUID id, @RequestBody SItem it) {
      it.setListId(id); it.setId(null); return items.save(it);
    }
    @GetMapping("/{id}/items") public List<SItem> list(@PathVariable UUID id) { return items.findByListId(id); }
    @PatchMapping("/items/{itemId}") public SItem patch(@PathVariable UUID itemId, @RequestBody Map<String, String> b) {
      var it = items.findById(itemId).orElseThrow(); it.setStatus(b.getOrDefault("status", it.getStatus()));
      return items.save(it);
    }
  }
}
