package in.mealwise.api.payments;

import in.mealwise.api.common.ApiException;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class Payments {
  private final String webhookSecret;
  public Payments(@Value("${RAZORPAY_WEBHOOK_SECRET:}") String s) { webhookSecret = s; }

  /** Razorpay webhook: verify HMAC-SHA256, then reconcile server-side. Idempotent on razorpay_payment_id. */
  @PostMapping("/razorpay/webhook")
  public Map<String, Object> webhook(@RequestBody String rawBody,
      @RequestHeader(value = "X-Razorpay-Signature", required = false) String sig) {
    if (webhookSecret == null || webhookSecret.isBlank())
      throw new ApiException("WEBHOOK_NOT_CONFIGURED", "Razorpay webhook secret missing.", HttpStatus.SERVICE_UNAVAILABLE);
    if (!verify(rawBody, sig)) throw new ApiException("BAD_SIGNATURE", "Invalid webhook signature.", HttpStatus.UNAUTHORIZED);
    // Production: parse event, upsert by provider_payment_id (UNIQUE), fetch payment from Razorpay server SDK, transition order.
    return Map.of("ok", true, "verified", true);
  }

  @GetMapping("/{id}")
  public Map<String, Object> status(@PathVariable String id) {
    // Production: fetch from Razorpay server-side; never trust client callback.
    return Map.of("paymentId", id, "status", "unknown-until-verified",
      "note", "Client callbacks are untrusted. Webhook + server fetch is authoritative.");
  }

  private boolean verify(String body, String sig) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256"));
      String hex = HexFormat.of().formatHex(mac.doFinal(body.getBytes()));
      return hex.equals(sig);
    } catch (Exception e) { return false; }
  }
}
