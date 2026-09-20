package com.example.app.integration;

import com.example.app.example.Note;
import com.example.app.example.NoteRepository;
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
}
