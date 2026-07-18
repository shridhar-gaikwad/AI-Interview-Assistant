package io.interviewAssistant.ai.application;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Thin wrapper around Spring AI's ChatClient. It sends a user's question to the
 * local Ollama model configured in application.yml and returns the reply.
 */
@Service
public class OllamaService {

    private final ChatClient chatClient;

    /**
     * Spring AI auto-configures a ChatClient.Builder from the spring.ai.ollama.*
     * properties. We inject that builder and build a reusable ChatClient once.
     */
    public OllamaService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /** Send a single question and return the whole answer at once (blocking). */
    public String askModel(String question) {
        return chatClient
                .prompt(question)   // set the user message
                .call()             // synchronous call to Ollama
                .content();         // extract the text of the reply
    }

    /**
     * Stream the answer token-by-token as a reactive Flux. Because tokens arrive
     * continuously, this is the natural fit for WebFlux and avoids the read
     * timeout that a slow, single-shot (blocking) response can hit.
     */
    public Flux<String> streamModel(String question) {
        return chatClient                   // LLM Abstraction Layer
                .prompt(question)
                .stream()           // reactive streaming call to Ollama
                .content();         // Flux<String> of response chunks
    }

    /**
     * Return the complete answer as a single String, but built by consuming the
     * streaming call above and joining its chunks. This gives us a clean, one-shot
     * response WITHOUT the read timeout that the blocking .call() hits on slow
     * (CPU-only) generations, because tokens keep arriving over the stream.
     */
    public Mono<String> askModelComplete(String question) {
        return streamModel(question)
                .collectList()                         // gather all chunks
                .map(chunks -> String.join("", chunks)); // join into full answer
    }
}
