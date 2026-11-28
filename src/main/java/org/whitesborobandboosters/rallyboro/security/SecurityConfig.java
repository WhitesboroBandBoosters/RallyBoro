package org.whitesborobandboosters.rallyboro.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {
  @Bean
  FilterRegistrationBean<RequestGuard> requestGuardRegistration(RequestGuard guard) {
    var registration = new FilterRegistrationBean<>(guard);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  public static String digest(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      JdbcTemplate jdbc,
      RequestGuard guard,
      @Value("${rallyboro.pilot-login:false}") boolean local,
      ObjectProvider<ClientRegistrationRepository> registrations)
      throws Exception {
    http.authorizeHttpRequests(a ->
        a.requestMatchers(
                "/auth/csrf",
                "/auth/options",
                "/login",
                "/oauth2/**",
                "/login/**",
                "/graphql",
                "/actuator/health/**",
                "/api/v1/status",
                "/",
                "/events/**",
                "/my-registrations",
                "/settings",
                "/assets/**",
                "/index.html",
                "/styles.css",
                "/error")
            .permitAll()
            .anyRequest()
            .denyAll());
    http.addFilterBefore(guard, CsrfFilter.class);
    http.csrf(c ->
        c.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            .ignoringRequestMatchers(r -> r.getAttribute("rallyboro.agent") != null));
    http.addFilterBefore(
        new OncePerRequestFilter() {
          @Override
          protected void doFilterInternal(
              HttpServletRequest request, HttpServletResponse response, FilterChain chain)
              throws ServletException, IOException {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
              var rows =
                  jdbc.queryForList(
                      "SELECT t.id,t.user_id,t.organization_id,t.write_enabled FROM agent_token t"
                          + " JOIN membership m ON m.user_id=t.user_id AND"
                          + " m.organization_id=t.organization_id WHERE token_hash=? AND NOT"
                          + " revoked AND expires_at>now()",
                      digest(header.substring(7)));
              if (rows.size() != 1) {
                response.sendError(401);
                return;
              }
              var row = rows.getFirst();
              var actor =
                  new Access.Actor(
                      (UUID) row.get("user_id"),
                      (UUID) row.get("organization_id"),
                      true,
                      (Boolean) row.get("write_enabled"));
              var context = SecurityContextHolder.createEmptyContext();
              context.setAuthentication(
                  new UsernamePasswordAuthenticationToken(actor, "", List.of()));
              SecurityContextHolder.setContext(context);
              request.setAttribute("rallyboro.agent", true);
            }
            try {
              chain.doFilter(request, response);
            } finally {
              if (request.getAttribute("rallyboro.agent") != null)
                SecurityContextHolder.clearContext();
            }
          }
        },
        CsrfFilter.class);
    if (local)
      http.formLogin(f ->
          f.loginPage("/login")
              .loginProcessingUrl("/auth/login")
              .successHandler((r, s, a) -> s.setStatus(204))
              .failureHandler((r, s, e) -> s.sendError(401)));
    if (registrations.getIfAvailable() != null)
      http.oauth2Login(o -> o.loginPage("/login").defaultSuccessUrl("/", true));
    http.logout(
        l -> l.logoutUrl("/auth/logout").logoutSuccessHandler((r, s, a) -> s.setStatus(204)));
    http.exceptionHandling(e -> e.authenticationEntryPoint((r, s, x) -> s.sendError(401)));
    http.headers(h ->
        h.contentSecurityPolicy(c ->
            c.policyDirectives(
                "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:;"
                    + " connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action"
                    + " 'self'")));
    return http.build();
  }

  @RestController
  static class AuthSupport {
    private final boolean local;
    private final ObjectProvider<ClientRegistrationRepository> registrations;

    AuthSupport(
        @Value("${rallyboro.pilot-login:false}") boolean local,
        ObjectProvider<ClientRegistrationRepository> registrations) {
      this.local = local;
      this.registrations = registrations;
    }

    @GetMapping("/auth/csrf")
    CsrfToken csrf(CsrfToken token) {
      return token;
    }

    @GetMapping("/auth/options")
    Object options() {
      return Map.of("local", local, "oidc", registrations.getIfAvailable() != null);
    }
  }
}
