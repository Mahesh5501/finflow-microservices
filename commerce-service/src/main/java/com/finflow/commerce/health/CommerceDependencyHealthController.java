package com.finflow.commerce.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Webapp dependency monitor — not JVM-wide metrics.
 *
 * Use {@code /actuator/prometheus} or {@code /actuator/metrics} for process metrics.
 */
@RestController
@RequestMapping("/api/v1/health")
public class CommerceDependencyHealthController {

    private final CommerceDependencyHealthService healthService;

    public CommerceDependencyHealthController(CommerceDependencyHealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/dependencies")
    public ResponseEntity<Map<String, Object>> dependencies() {
        Map<String, Object> body = healthService.snapshot();
        String status = String.valueOf(body.get("status"));
        return "DOWN".equals(status)
                ? ResponseEntity.status(503).body(body)
                : ResponseEntity.ok(body);
    }
}
