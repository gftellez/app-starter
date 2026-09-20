package com.example.app.tenant;

import java.util.function.Supplier;

/**
 * The tenant the current work belongs to.
 *
 * A static holder because JPA entity listeners are not Spring beans; {@link TenantContext}
 * binds the resolver at startup. The scope is the thread, which is also the scope of one
 * request and of one background job.
 */
public final class CurrentTenant {

    private CurrentTenant() {}

    private static final ThreadLocal<Tenant> OVERRIDE = new ThreadLocal<>();
    private static volatile Supplier<Tenant> fallback = () -> null;

    static void bindFallback(Supplier<Tenant> supplier) {
        fallback = supplier;
    }

    /** Runs the action as the given tenant, restoring whatever was set before. */
    public static void runAs(Tenant tenant, Runnable action) {
        Tenant previous = OVERRIDE.get();
        OVERRIDE.set(tenant);
        try {
            action.run();
        } finally {
            if (previous == null) OVERRIDE.remove(); else OVERRIDE.set(previous);
        }
    }

    public static Tenant get() {
        Tenant explicit = OVERRIDE.get();
        return explicit != null ? explicit : fallback.get();
    }

    public static Tenant require() {
        Tenant tenant = get();
        if (tenant == null) {
            throw new IllegalStateException(
                    "No tenant in context. Until sign-in resolves one per user, this only works "
                    + "while a single tenant exists — see TenantContext.");
        }
        return tenant;
    }
}
