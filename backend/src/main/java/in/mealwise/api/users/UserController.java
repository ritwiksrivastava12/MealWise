package in.mealwise.api.users;

import in.mealwise.api.config.JwtFilter;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {
  private final UserRepository users; private final UserPreferencesRepository prefs;
  public UserController(UserRepository u, UserPreferencesRepository p) { users = u; prefs = p; }

  @GetMapping public Map<String, Object> me() {
    var u = users.findById(UUID.fromString(JwtFilter.userId())).orElseThrow();
    var p = prefs.findById(u.getId()).orElse(UserPreferences.builder().userId(u.getId()).build());
    return Map.of("userId", u.getId().toString(), "email", mask(u.getEmail()),
      "phone", mask(u.getPhoneE164()), "preferences", p);
  }
  @PutMapping("/preferences")
  public UserPreferences savePrefs(@RequestBody UserPreferences p) {
    UUID uid = UUID.fromString(JwtFilter.userId());
    p.setUserId(uid);
    if (!List.of("vegetarian", "non_veg", "eggetarian").contains(p.getDiet()))
      throw new IllegalArgumentException("diet must be vegetarian|non_veg|eggetarian");
    return prefs.save(p);
  }
  @GetMapping("/export")
  public Map<String, Object> export() {
    // DPDP: machine-readable export (full export job streams rows in prod).
    return me();
  }
  private static String mask(String v) { return v == null ? null : v; } // masking for support roles happens in admin layer
}
