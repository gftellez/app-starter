package com.example.app.config;

import com.example.app.health.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import java.net.ServerSocket;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The template's safety defaults, each pinned so a later edit cannot quietly drop one. */
class StartupAndHttpTest {

    @Test
    void thePlaceholderToken_orPassword_stopsTheAppFromStarting() {
        assertThatThrownBy(() -> new DefaultSecretsCheck("changeme", "real"))
                .hasMessageContaining("APP_AUTH_TOKEN");
        assertThatThrownBy(() -> new DefaultSecretsCheck("real", "changeme"))
                .hasMessageContaining("DB_PASSWORD");
        assertThatThrownBy(() -> new DefaultSecretsCheck(" ", "real"))
                .hasMessageContaining("APP_AUTH_TOKEN");
        new DefaultSecretsCheck("a-real-token", "a-real-password");   // no exception
    }

    @Test
    void anUpstreamThatNeverAnswers_timesOut_insteadOfHoldingTheThread() throws Exception {
        try (ServerSocket silent = new ServerSocket(0)) {   // accepts the connection, never replies
            var client = new HttpClientConfig()
                    .restClientBuilder(Duration.ofSeconds(1), Duration.ofMillis(500))
                    .baseUrl("http://127.0.0.1:" + silent.getLocalPort())
                    .build();
            long start = System.nanoTime();

            assertThatThrownBy(() -> client.get().uri("/").retrieve().toBodilessEntity())
                    .isInstanceOf(ResourceAccessException.class);
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(5));
        }
    }

    @Test
    void theHealthCheck_needsNoToken_butEverythingElseUnderApiDoes() throws Exception {
        AuthFilter filter = new AuthFilter();
        ReflectionTestUtils.setField(filter, "authToken", "secret");

        MockFilterChain healthChain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/health"), new MockHttpServletResponse(), healthChain);
        assertThat(healthChain.getRequest()).isNotNull();          // passed through

        MockHttpServletResponse denied = new MockHttpServletResponse();
        MockFilterChain notesChain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/notes"), denied, notesChain);
        assertThat(denied.getStatus()).isEqualTo(401);
        assertThat(notesChain.getRequest()).isNull();               // stopped
    }
}
