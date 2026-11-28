package org.whitesborobandboosters.rallyboro;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class FoundationController {
  @GetMapping({
    "/",
    "/login",
    "/events",
    "/events/{id}",
    "/events/{id}/edit",
    "/my-registrations",
    "/settings"
  })
  public String home() {
    return "forward:/index.html";
  }

  @GetMapping("/api/v1/status")
  @ResponseBody
  public ApplicationStatus status() {
    return new ApplicationStatus("RallyBoro", "staffing-pilot");
  }

  public record ApplicationStatus(String name, String stage) {}
}
