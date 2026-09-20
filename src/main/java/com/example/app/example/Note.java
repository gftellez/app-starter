package com.example.app.example;

import com.example.app.tenant.Tenant;
import com.example.app.tenant.TenantOwned;
import com.example.app.tenant.TenantStamper;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * The one example entity, here to show the tenant pattern end to end — and to give the
 * integration test something real to stamp. Delete it, and its repository, controller,
 * migration table and test, once the app has entities of its own.
 *
 * The two lines that matter and get copied to every owned entity: {@code @EntityListeners}
 * and the {@code tenant} column, non-null.
 */
@Entity
@Table(name = "notes")
@EntityListeners(TenantStamper.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note implements TenantOwned {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void defaults() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
