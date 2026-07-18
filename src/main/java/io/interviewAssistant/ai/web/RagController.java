package io.interviewAssistant.ai.web;

import io.interviewAssistant.ai.rag.IngestionService;
import io.interviewAssistant.ai.rag.RagService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * HTTP entry points for RAG (Retrieval-Augmented Generation):
 *  - ingest documents into the vector store
 *  - ask questions answered from those documents
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final IngestionService ingestionService;
    private final RagService ragService;

    public RagController(IngestionService ingestionService, RagService ragService) {
        this.ingestionService = ingestionService;
        this.ragService = ragService;
    }

    /**
     * Trigger ingestion of the configured documents folder into the vector store.
     * Ingestion is blocking (JDBC + embeddings), so we offload it to the
     * boundedElastic scheduler to avoid blocking the Netty event loop.
     * Returns a small summary of how many chunks were stored.
     */
    @PostMapping("/ingest")
    public Mono<IngestResponse> ingest() {
        return Mono.fromCallable(ingestionService::ingestFromConfiguredLocation)
                .subscribeOn(Schedulers.boundedElastic())
                .map(count -> new IngestResponse(count, "Ingestion complete."));
    }

    /**
     * Streaming RAG answer as plain text: open
     * GET /api/rag/ask?question=... and watch the grounded answer build up.
     */
    @GetMapping(value = "/ask", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> ask(@RequestParam String question) {
        return ragService.ragStream(question);
    }

    /**
     * Full grounded answer: POST /api/rag/ask with {"question": "..."} returns
     * {"question": "...", "answer": "..."} in one JSON response.
     */
    @PostMapping("/ask")
    public Mono<AskResponse> ask(@RequestBody AskRequest request) {
        return ragService.ragComplete(request.question())
                .map(answer -> new AskResponse(request.question(), answer));
    }

    /** Incoming JSON payload for the POST endpoint. */
    public record AskRequest(String question) {
    }

    /** Response payload returned to the caller (serialized to JSON). */
    public record AskResponse(String question, String answer) {
    }

    /** Summary returned after ingestion. */
    public record IngestResponse(int chunksStored, String message) {
    }
}
