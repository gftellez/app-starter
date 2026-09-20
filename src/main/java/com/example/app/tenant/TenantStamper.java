package com.example.app.tenant;

import jakarta.persistence.PrePersist;

/**
 * Stamps every new row with the tenant it belongs to.
 *
 * Done here rather than at each call site on purpose: rows get created in dozens of places,
 * and one forgotten assignment is a row that belongs to nobody — or, once a second tenant
 * exists, to the wrong one. A listener cannot be forgotten.
 *
 * Use it as:  {@code @EntityListeners(TenantStamper.class)}
 */
public class TenantStamper {

    @PrePersist
    void stamp(Object entity) {
        if (entity instanceof TenantOwned owned && owned.getTenant() == null) {
            owned.setTenant(CurrentTenant.require());
        }
    }
}
