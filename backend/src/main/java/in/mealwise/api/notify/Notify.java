package in.mealwise.api.notify;

import in.mealwise.api.config.JwtFilter;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class Notify {
  private final List<Map<String, Object>> outbox = Collections.synchronizedList(new ArrayList<>());
  @PostMapping("/devices/fcm-token")
  public Map<String, Object> token(@RequestBody Map<String, String> b) {
    // Production: hash + store in device_tokens; validate via Firebase Admin SDK.
    return Map.of("ok", true, "user", JwtFilter.userId());
  }
  @GetMapping("/notifications/preferences")
  public Map<String, Object> prefs() {
    return Map.of("planner", true, "expiry", true, "orders", true, "marketing", false);
  }
}
