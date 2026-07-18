package io.interviewAssistant.ai.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Answers questions using RAG (Retrieval-Augmented Generation).
 *
 * A {@link QuestionAnswerAdvisor} sits in front of the ChatClient: before each
 * call it runs a similarity search over the vector store, then injects the most
 * relevant document chunks into the prompt as context. The model therefore
 * answers from OUR ingested interview material rather than only its training data.
 */
@Service
public class RagService {

    /**
     * Instructs the model to answer strictly from the retrieved context, so it
     * grounds responses in our documents instead of hallucinating.
     */
    private static final String SYSTEM_PROMPT = """
            You are an AI interview preparation assistant.
            Answer the question using ONLY the context provided to you.
            If the answer is not contained in the context, say you don't have
            enough information in the knowledge base to answer confidently.
            Keep answers clear, correct, and concise.
            """;

    private final ChatClient chatClient;

    public RagService(ChatClient.Builder chatClientBuilder,
                      VectorStore vectorStore,
                      @Value("${app.rag.top-k}") int topK,
                      @Value("${app.rag.similarity-threshold}") double similarityThreshold) {

        // Controls how the advisor retrieves context: how many chunks (topK) and
        // how strict the relevance cut-off (similarityThreshold) should be.
        SearchRequest searchRequest = SearchRequest.builder()
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();

        QuestionAnswerAdvisor ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .build();

        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(ragAdvisor)
                .build();
    }

    /**
     * Stream a grounded answer token-by-token. The advisor's similarity search is
     * blocking JDBC, so we subscribe on the boundedElastic scheduler to keep it
     * off the Netty event loop.
     */
    public Flux<String> ragStream(String question) {
        return chatClient.prompt()
                .user(question)
                .stream()
                .content()
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** Return the full grounded answer as a single String (stream then join). */
    public Mono<String> ragComplete(String question) {
        return ragStream(question)
                .collectList()
                .map(chunks -> String.join("", chunks));
    }
}
