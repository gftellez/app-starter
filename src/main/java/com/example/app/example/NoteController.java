package com.example.app.example;

import com.example.app.tenant.CurrentTenant;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * The example endpoint. Delete it along with {@link Note}.
 *
 * It is {@code @Transactional(readOnly = true)} on the read for a reason worth keeping:
 * {@code spring.jpa.open-in-view} is off, so reading a lazy association outside a
 * transaction throws and the endpoint answers 500. Where the read is followed by something
 * slow (an HTTP call to another service), prefer an {@code @EntityGraph} on the query over
 * a transaction that holds a database connection for the whole round trip.
 */
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteRepository notes;

    @GetMapping
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return notes.findByTenantIdOrderByCreatedAtDesc(CurrentTenant.require().getId())
                .stream()
                .map(n -> Map.<String, Object>of(
                        "id", n.getId(),
                        "text", n.getText(),
                        "createdAt", n.getCreatedAt()))
                .toList();
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
        // No tenant set here on purpose — TenantStamper fills it in on insert.
        Note saved = notes.save(Note.builder().text(text.trim()).build());
        return Map.of("id", saved.getId(), "text", saved.getText(), "createdAt", saved.getCreatedAt());
    }
}
