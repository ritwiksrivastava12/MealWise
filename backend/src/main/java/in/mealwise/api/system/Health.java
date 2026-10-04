package in.mealwise.api.system;

import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class Health {
  @GetMapping("/health") public Map<String, Object> health() { return Map.of("status", "UP", "service", "mealwise-api"); }
}
