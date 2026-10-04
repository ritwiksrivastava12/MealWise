package in.mealwise.api.config;

import in.mealwise.api.common.CorrelationFilter;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
  @Bean PasswordEncoder passwordEncoder() {
    // Argon2id (adaptive). Spring's Argon2PasswordEncoder defaults: salt 16, hash 32, parallelism 1, memory 1<<12, iterations 3.
    return new Argon2PasswordEncoder(16, 32, 1, 1 << 14, 2);
  }
  @Bean SecurityFilterChain chain(HttpSecurity h, JwtFilter jwt, CorrelationFilter cid) throws Exception {
    return h.csrf(c -> c.disable())
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(a -> a
        .requestMatchers("/api/v1/health", "/api/v1/auth/**", "/v3/api-docs/**", "/swagger-ui/**",
          "/actuator/health", "/api/v1/payments/razorpay/webhook", "/api/v1/subscriptions/play-rtdn").permitAll()
        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated())
      .addFilterBefore(cid, UsernamePasswordAuthenticationFilter.class)
      .addFilterAfter(jwt, CorrelationFilter.class)
      .build();
  }
}
