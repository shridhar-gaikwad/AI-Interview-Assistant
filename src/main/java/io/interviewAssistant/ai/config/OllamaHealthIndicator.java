package io.interviewAssistant.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Reports Ollama's reachability at /actuator/health by pinging its /api/tags
 * endpoint (lightweight - just lists installed models). Uses its own short
 * timeout, separate from the generous 5-minute read timeout HttpClientConfig
 * sets for actual chat calls, so a down server fails the health check in
 * seconds instead of minutes.
 */
@Component
public class OllamaHealthIndicator implements HealthIndicator {

    private final RestClient restClient;
    private final String baseUrl;

    public OllamaHealthIndicator(@Value("${spring.ai.ollama.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(2))
                .withReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.jdk().build(settings))
                .build();
    }

    @Override
    public Health health() {
        try {
            restClient.get().uri("/api/tags").retrieve().toBodilessEntity();
            return Health.up().withDetail("baseUrl", baseUrl).build();
        } catch (Exception ex) {
            return Health.down(ex).withDetail("baseUrl", baseUrl).build();
        }
    }
}
