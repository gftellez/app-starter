package com.example.app.integration;

import com.example.app.example.Note;
import com.example.app.example.NoteController;
import com.example.app.example.NoteRepository;
import com.example.app.tenant.CurrentTenant;
import com.example.app.tenant.Tenant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The one test that proves the template's central mechanism: a row saved without anyone
 * setting a tenant still ends up owned. Delete it with the example entity — but write its
 * equivalent for whatever replaces it.
 */
class TenantStampIntegrationTest extends BaseIntegrationTest {

    @Autowired NoteRepository notes;
    @Autowired NoteController controller;
    @Autowired com.example.app.health.HealthController health;

    @Test
    @DisplayName("a row saved with no tenant set is stamped with the current one")
    void stampsOnInsert() {
        Note saved = notes.save(Note.builder().text("written by nobody in particular").build());

        assertThat(saved.getTenant()).isNotNull();
        assertThat(saved.getTenant().getId()).isEqualTo(tenant.getId());
    }

    @Test
    @DisplayName("the scoped query only returns this tenant's rows")
    void queryIsScoped() {
        notes.save(Note.builder().text("mine").build());

        assertThat(notes.findByTenantIdOrderByCreatedAtDesc(tenant.getId()))
                .extracting(Note::getText)
                .containsExactly("mine");
    }

    @Test
    @DisplayName("another tenant's row, asked for by its id, does not exist")
    void byIdIsScoped() {
        Tenant other = tenants.save(Tenant.builder().name("Someone else").build());
        Note[] theirs = new Note[1];
        CurrentTenant.runAs(other, () -> theirs[0] = notes.save(Note.builder().text("not yours").build()));

        CurrentTenant.runAs(tenant, () -> {
            assertThat(controller.get(theirs[0].getId()).getStatusCode().value()).isEqualTo(404);
            assertThat(controller.delete(theirs[0].getId()).getStatusCode().value()).isEqualTo(404);
        });
        assertThat(notes.findById(theirs[0].getId())).isPresent();
    }

    @Test
    @DisplayName("the health check answers, database included")
    void healthIsUp() {
        assertThat(health.health().getStatusCode().value()).isEqualTo(200);
    }
}
