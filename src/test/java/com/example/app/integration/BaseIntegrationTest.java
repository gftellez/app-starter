package com.example.app.integration;

import com.example.app.tenant.Tenant;
import com.example.app.tenant.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;

/**
 * Base for tests that need a real database.
 *
 * Two deliberate choices:
 *
 * <p><b>A real PostgreSQL, not an in-memory stand-in.</b> The behaviour that breaks in
 * production — the SQL a query really generates, what the driver does with a type, how a
 * constraint fires — is exactly what a substitute database does differently.
 *
 * <p><b>These tests are not {@code @Transactional}.</b> A test wrapped in a transaction
 * keeps a session open, which hides every lazy-loading bug: the association loads fine in
 * the test and throws in the request. Here a controller that reads a lazy association
 * without a transaction fails the same way it would in production. That is the point, and
 * it has caught real 500s.
 *
 * <p>The container is static, so it starts once for the whole suite; the trade is that rows
 * leak between classes unless every table is truncated first, which is what {@link #reset()}
 * does.
 */
@SpringBootTest
public abstract class BaseIntegrationTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine").withReuse(true);

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // One role owns everything in a container, so migrations use the same credentials.
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("app.auth.token", () -> "test-token");
    }

    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected TenantRepository tenants;

    protected Tenant tenant;

    @BeforeEach
    void reset() {
        List<String> tables = jdbc.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public' "
                + "AND tablename <> 'flyway_schema_history'", String.class);
        if (!tables.isEmpty()) {
            jdbc.execute("TRUNCATE TABLE " + String.join(", ", tables) + " CASCADE");
        }
        // Exactly one tenant exists after this, which is what TenantContext resolves to —
        // so the stamp works in tests through the same path it uses in production.
        tenant = tenants.save(Tenant.builder().name("Test tenant").build());
    }
}
