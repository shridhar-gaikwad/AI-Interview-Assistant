package io.interviewAssistant.ai.config;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Customizes the HTTP client that Spring AI uses to call Ollama.
 *
 * Spring AI talks to Ollama through an auto-configured RestClient. By default,
 * with WebFlux on the classpath, that RestClient uses the Reactor Netty connector,
 * whose ReadTimeoutHandler aborts a request if no bytes arrive within a short
 * window. A CPU-only 7B model can take much longer than that to generate a full
 * (non-streamed) answer, causing ReadTimeoutException.
 *
 * We register a RestClientCustomizer (applied to every auto-configured RestClient,
 * including Spring AI's) that swaps in the JDK HttpClient factory with a generous
 * 5-minute read timeout, which reliably honors the configured duration.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClientCustomizer ollamaTimeoutCustomizer() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(30))
                .withReadTimeout(Duration.ofMinutes(5));
        return builder -> builder.requestFactory(ClientHttpRequestFactoryBuilder.jdk().build(settings));
    }
}
