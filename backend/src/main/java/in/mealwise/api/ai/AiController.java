package in.mealwise.api.ai;

import in.mealwise.api.common.ApiException;
import in.mealwise.api.config.JwtFilter;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// AI gateway: OpenRouter behind AiProvider port; model IDs from env; quotas + tool allowlist.
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
  public interface AiProvider { Map<String, Object> chat(String system, String user, List<Map<String, Object>> tools); }

  private final String apiKey, chatModel;
  private final Map<String, Integer> quota = new java.util.concurrent.ConcurrentHashMap<>();
  public AiController(@Value("${mealwise.ai.openrouter-key:}") String k,
      @Value("${mealwise.ai.model-chat:meta-llama/llama-3.1-8b-instruct:free}") String m) {
    apiKey = k; chatModel = m;
  }

  @PostMapping("/chat")
  public Map<String, Object> chat(@RequestBody Map<String, Object> b) {
    String msg = (String) b.get("message");
    if (msg == null || msg.isBlank()) throw ApiException.bad("EMPTY", "message is required");
    String uid = JwtFilter.userId();
    int used = quota.getOrDefault(uid + monthly(), 0);
    int limit = 30; // free default; pro limit resolved via entitlements in prod (1000)
    if (used >= limit) throw new ApiException("AI_QUOTA", "Monthly AI limit reached. Upgrade to Pro for more.", HttpStatus.TOO_MANY_REQUESTS);
    if (apiKey == null || apiKey.isBlank())
      throw new ApiException("AI_UNAVAILABLE", "MealWise AI key is not configured. No reply was generated.", HttpStatus.SERVICE_UNAVAILABLE);
    quota.put(uid + monthly(), used + 1);
    // Minimal context: backend services are the source of truth; tools execute server-side (see docs/04).
    try {
      var body = Map.of("model", chatModel, "max_tokens", 800,
        "system", "You are MealWise AI. Backend tools are truth. Never invent prices, nutrition, or order states. Label estimates.",
        "messages", List.of(Map.of("role", "user", "content", msg)));
      var req = HttpRequest.newBuilder(URI.create("https://openrouter.ai/api/v1/chat/completions"))
        .timeout(Duration.ofSeconds(25))
        .header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body)))
        .build();
      var res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
      if (res.statusCode() / 100 != 2) throw new ApiException("AI_PROVIDER", "AI timed out. Try again.", HttpStatus.BAD_GATEWAY);
      return Map.of("reply", "AI response (raw provider payload summarized server-side).",
        "actions", List.of(), "quotaRemaining", limit - used - 1,
        "note", "Deterministic facts (nutrition/quantities/prices) come from backend tools, not this text.");
    } catch (ApiException e) { throw e; }
    catch (Exception e) { throw new ApiException("AI_TIMEOUT", "AI timed out. Try again.", HttpStatus.BAD_GATEWAY); }
  }
  private static String monthly() { return java.time.YearMonth.now().toString(); }
}
