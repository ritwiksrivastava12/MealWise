package in.mealwise.api.users;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity @Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
  @Id private UUID id;
  @Column(unique = true) private String email;      // CITEXT in DDL
  @Column(name = "phone_e164", unique = true) private String phoneE164;
  @Column(name = "password_hash") private String passwordHash;
  @Builder.Default private String status = "active";
  @Column(name = "created_at") private Instant createdAt;
  @Column(name = "updated_at") private Instant updatedAt;
  @PrePersist void pre() { if (id == null) id = UUID.randomUUID(); createdAt = updatedAt = Instant.now(); }
  @PreUpdate void upd() { updatedAt = Instant.now(); }
}
