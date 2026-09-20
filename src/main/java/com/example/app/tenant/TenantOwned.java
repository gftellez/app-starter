package com.example.app.tenant;

/**
 * A row that belongs to exactly one tenant. Every entity that holds tenant data implements
 * this and registers {@link TenantStamper} as an {@code @EntityListeners}.
 */
public interface TenantOwned {
    Tenant getTenant();
    void setTenant(Tenant tenant);
}
