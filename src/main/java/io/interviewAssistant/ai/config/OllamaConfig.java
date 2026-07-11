package io.interviewAssistant.ai.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaConfig {

    @Bean
    public ChatClient qwenClient(OllamaApi ollamaApi, ObservationRegistry observationRegistry) {
        OllamaChatModel qwenModel = new OllamaChatModel(
                ollamaApi,
                OllamaOptions.builder().model("qwen2.5:7b").build(),
                null,
                observationRegistry,
                null
        );
        return ChatClient.builder(qwenModel).build();
    }

    @Bean
    public ChatClient llamaClient(OllamaApi ollamaApi, ObservationRegistry observationRegistry) {
        OllamaChatModel llamaModel = new OllamaChatModel(
                ollamaApi,
                OllamaOptions.builder().model("llama3").build(),
                null,
                observationRegistry,
                null
        );
        return ChatClient.builder(llamaModel).build();
    }
}


