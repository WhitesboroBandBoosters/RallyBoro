package org.whitesborobandboosters.rallyboro.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Small single-instance pilot limits; replace with shared limits if deploying replicas. */
@Component
public class RequestGuard extends OncePerRequestFilter {
  private record Window(long minute, int count) {}

  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getServletPath();
    if (path.equals("/graphql") || path.equals("/auth/login")) {
      if (!request.getMethod().equals("POST")) {
        response.sendError(405);
        return;
      }
      if (request.getContentLengthLong() < 0 || request.getContentLengthLong() > 65536) {
        response.sendError(413);
        return;
      }
      long minute = System.currentTimeMillis() / 60000;
      if (windows.size() > 10000) {
        windows.entrySet().removeIf(e -> e.getValue().minute() < minute);
        if (windows.size() > 10000) {
          response.sendError(429);
          return;
        }
      }
      String key = request.getRemoteAddr() + path;
      Window window =
          windows.compute(
              key,
              (k, w) -> new Window(minute, w == null || w.minute() != minute ? 1 : w.count() + 1));
      int max = path.equals("/auth/login") ? 60 : 600;
      if (window.count() > max) {
        response.setHeader("Retry-After", "60");
        response.sendError(429);
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
