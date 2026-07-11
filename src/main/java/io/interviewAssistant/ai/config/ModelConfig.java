package io.interviewAssistant.ai.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ModelConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public ResponseErrorHandler responseErrorHandler() {
        return new DefaultResponseErrorHandler();
    }

    @Bean
    public ObservationRegistry observationRegistry() {
        return ObservationRegistry.create();
    }

    @Bean
    public org.springframework.ai.ollama.api.OllamaApi ollamaApi(RestClient.Builder restClientBuilder,
                               WebClient.Builder webClientBuilder,
                               ResponseErrorHandler errorHandler) {
        try {
            java.lang.reflect.Constructor<?> ctor = org.springframework.ai.ollama.api.OllamaApi.class
                    .getDeclaredConstructor(String.class, RestClient.Builder.class, WebClient.Builder.class, ResponseErrorHandler.class);
            ctor.setAccessible(true);
            return (org.springframework.ai.ollama.api.OllamaApi) ctor.newInstance("http://localhost:11434", restClientBuilder, webClientBuilder, errorHandler);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create OllamaApi instance", ex);
        }
    }

}