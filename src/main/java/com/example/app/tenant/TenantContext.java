package com.example.app.tenant;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves which tenant the current work belongs to.
 *
 * While exactly one exists it resolves to that one, so a single-tenant app needs no
 * ceremony. It deliberately <em>refuses to guess</em> once a second appears: from that
 * moment every request has to say who it is for, and a silent default is how one customer
 * ends up reading another's data. Sign-in is what fills that in — resolve the tenant from
 * the authenticated user here and the stamp keeps working unchanged.
 *
 * Note what this is not: isolation enforced by the database. Until PostgreSQL row-level
 * security is switched on, a query that forgets its tenant filter still reads everything.
 */
@Component
@RequiredArgsConstructor
public class TenantContext {

    private final TenantRepository tenants;

    @PostConstruct
    void bind() {
        CurrentTenant.bindFallback(this::onlyTenant);
    }

    private Tenant onlyTenant() {
        List<Tenant> all = tenants.findAll();
        if (all.size() == 1) return all.getFirst();
        if (all.isEmpty()) return null;
        throw new IllegalStateException(
                "More than one tenant exists, so there is no single default. Resolve the tenant "
                + "from the signed-in user before reading or writing.");
    }
}
