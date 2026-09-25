package com.example.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Refuses to start with the placeholder secrets from application.properties.
 *
 * Both fall back to "changeme" so a laptop can boot without an env file. On a server that
 * fallback is a trap: a missing or misspelled variable brings the app up with a token anyone
 * can guess, and nothing says so. Startup is the one moment someone is watching.
 */
@Component
class DefaultSecretsCheck {

    private static final String PLACEHOLDER = "changeme";

    DefaultSecretsCheck(@Value("${app.auth.token}") String authToken,
                        @Value("${spring.datasource.password}") String dbPassword) {
        if (isPlaceholder(authToken)) {
            throw new IllegalStateException(
                    "APP_AUTH_TOKEN is not set (it is still the placeholder). Set it in the env file.");
        }
        if (isPlaceholder(dbPassword)) {
            throw new IllegalStateException(
                    "DB_PASSWORD is not set (it is still the placeholder). Set it in the env file.");
        }
    }

    private static boolean isPlaceholder(String value) {
        return value == null || value.isBlank() || PLACEHOLDER.equals(value.trim());
    }
}
