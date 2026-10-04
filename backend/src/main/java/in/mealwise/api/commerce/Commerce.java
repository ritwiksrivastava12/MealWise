package in.mealwise.api.commerce;

import in.mealwise.api.common.ApiException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

// Provider-independent port. Core never branches on provider names.
public interface CommerceProvider {
  String name();
  Set<String> capabilities();
  Map<String, Object> handoff(Map<String, Object> req);
}

@Service
class CommerceRegistry {
  private final boolean sandbox; private final boolean swiggy; private final boolean ondc;
  CommerceRegistry(@Value("${mealwise.commerce.sandbox.enabled:false}") boolean s,
      @Value("${mealwise.commerce.swiggy.enabled:false}") boolean sw,
      @Value("${mealwise.commerce.ondc.enabled:false}") boolean o) {
    sandbox = s; swiggy = sw; ondc = o;
  }
  List<Map<String, Object>> providers() {
    var out = new ArrayList<Map<String, Object>>();
    out.add(Map.of("name", "handoff", "capabilities", List.of("HANDOFF"),
      "enabled", true, "note", "Lawful deep-link to provider apps. No live prices."));
    out.add(Map.of("name", "swiggy", "capabilities", List.of("HANDOFF", "QUOTE", "STATUS"),
      "enabled", swiggy, "note", swiggy ? "Live (authorized)" : "Requires Swiggy Builders/MCP prod approval"));
    out.add(Map.of("name", "ondc", "capabilities", List.of("SEARCH", "QUOTE", "STATUS", "CANCEL"),
      "enabled", ondc, "note", ondc ? "Live (registered buyer)" : "Requires ONDC registration + spec freeze"));
    return out;
  }
  boolean live(String name) {
    return switch (name) { case "swiggy" -> swiggy; case "ondc" -> ondc; default -> false; };
  }
  boolean isSandbox() { return sandbox; }
}

@RestController
@RequestMapping("/api/v1/commerce")
class CommerceController {
  private final CommerceRegistry reg;
  CommerceController(CommerceRegistry r) { reg = r; }

  @GetMapping("/providers") public List<Map<String, Object>> providers() { return reg.providers(); }

  @PostMapping("/handoff")
  public Map<String, Object> handoff(@RequestBody Map<String, Object> b) {
    String provider = (String) b.getOrDefault("provider", "swiggy");
    String query = (String) b.getOrDefault("query", "paneer");
    String q = URLEncoder.encode(query, StandardCharsets.UTF_8);
    String url = switch (provider) {
      case "zomato" -> "https://www.zomato.com/search?q=" + q;
      case "blinkit" -> "https://blinkit.com/s/?q=" + q;
      default -> "https://www.swiggy.com/search?query=" + q;
    };
    return Map.of("handoffUrl", url, "provider", provider,
      "disclaimer", "You’re opening " + provider + ". MealWise doesn’t control prices or availability there.");
  }

  @PostMapping("/quotes")
  public Map<String, Object> quotes(@RequestBody Map<String, Object> b) {
    String provider = (String) b.getOrDefault("provider", "swiggy");
    if (reg.live(provider)) throw ApiException.bad("NOT_WIRED", "Live adapter enabled but provider client not configured in this drop.");
    if (reg.isSandbox() && "sandbox".equals(provider))
      return Map.of("quotes", List.of(), "sourceType", "test", "freshness", "test",
        "note", "Sandbox quotes for E2E only. Never shown as live.");
    throw ApiException.unavailable("Live ordering for '" + provider + "' is not authorized yet. Use handoff instead — no live prices are shown.");
  }
}
