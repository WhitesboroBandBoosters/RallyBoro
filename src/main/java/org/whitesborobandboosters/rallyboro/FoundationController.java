package org.whitesborobandboosters.rallyboro;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class FoundationController {
    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/api/v1/status")
    @ResponseBody
    public ApplicationStatus status() {
        return new ApplicationStatus("RallyBoro", "foundation");
    }

    public record ApplicationStatus(String name, String stage) { }
}
