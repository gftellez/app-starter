package com.example.app.example;

import com.example.app.tenant.CurrentTenant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    /**
     * The by-id pattern every owned entity copies: look it up, then check it is this tenant's.
     * Another tenant's id answers 404, not 403 — from here, it does not exist.
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> get(@PathVariable UUID id) {
        return notes.findById(id)
                .filter(CurrentTenant::owns)
                .map(n -> ResponseEntity.ok(Map.<String, Object>of(
                        "id", n.getId(), "text", n.getText(), "createdAt", n.getCreatedAt())))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        var note = notes.findById(id).filter(CurrentTenant::owns);
        if (note.isEmpty()) return ResponseEntity.notFound().build();
        notes.delete(note.get());
        return ResponseEntity.noContent().build();
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
