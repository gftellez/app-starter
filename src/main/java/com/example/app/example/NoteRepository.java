package com.example.app.example;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NoteRepository extends JpaRepository<Note, UUID> {

    /**
     * Scoped by tenant, like every query in a multi-tenant app has to be. The stamp puts the
     * column there; only the query keeps one tenant from reading another's rows.
     */
    List<Note> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
