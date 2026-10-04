package in.mealwise.api.common;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationFilter extends OncePerRequestFilter {
  public static String current() { String v = MDC.get("cid"); return v == null ? "-" : v; }
  @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String cid = req.getHeader("X-Correlation-ID");
    if (cid == null || cid.isBlank()) cid = UUID.randomUUID().toString();
    MDC.put("cid", cid);
    res.setHeader("X-Correlation-ID", cid);
    try { chain.doFilter(req, res); } finally { MDC.remove("cid"); }
  }
}
