package com.example.app.health;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * {@code GET /api/health} — public, and it touches the database.
 *
 * A deploy is only done when the new process answers. Checking "is something listening" is
 * not enough: behind nginx, a path the backend does not serve can be answered by the frontend
 * with a 200 while the backend is still starting — a false "all good" that has happened. This
 * lives under /api so it always reaches the backend, and it runs a query so a process that
 * started but cannot reach its database does not count as up.
 */
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbc;

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("status", "database unreachable"));
        }
    }
}
