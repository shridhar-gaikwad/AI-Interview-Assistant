package io.interviewAssistant.ai.application;

import io.interviewAssistant.ai.config.ModelConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class OllamaService {

    private final ChatClient chatClient;

    public OllamaService(@Qualifier("qwenClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String askModel(String question) {
        return chatClient.prompt(question).call().content();
    }
}

