package io.interviewAssistant.ai.web;

import io.interviewAssistant.ai.application.OllamaService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * HTTP entry point for the assistant. It takes a question, delegates to
 * OllamaService (which calls the local LLM), and returns the answer.
 *
 * We run on WebFlux/Netty. The streaming endpoint is the natural reactive fit;
 * the blocking endpoint is kept for comparison and offloads its blocking call
 * onto Schedulers.boundedElastic() so it never blocks the Netty event loop.
 */
@RestController
@RequestMapping("/api")
public class InterviewController {

    private final OllamaService ollamaService;

    public InterviewController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    /**
     * Streaming answer, token-by-token, as plain text. Browser-friendly: open
     * GET /api/ask?question=... and watch the answer build up as normal flowing
     * text. We use text/plain (not text/event-stream) so there are no "data:"
     * SSE prefixes or one-word-per-line formatting.
     */
    @GetMapping(value = "/ask", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> ask(@RequestParam String question) {
        return ollamaService.streamModel(question);
    }

    /**
     * Full-answer variant: POST /api/ask with JSON {"question": "..."} returns the
     * complete answer as JSON {"question": "...", "answer": "..."} in one response.
     * Internally it consumes the streaming call and aggregates the tokens, which
     * avoids the read timeout a slow blocking (.call()) response would hit.
     */
    @PostMapping("/ask")
    public Mono<AskResponse> ask(@RequestBody AskRequest request) {
        return ollamaService.askModelComplete(request.question())
                .map(answer -> new AskResponse(request.question(), answer));
    }

    /** Incoming JSON payload for the POST endpoint. */
    public record AskRequest(String question) {
    }

    /** Response payload returned to the caller (serialized to JSON). */
    public record AskResponse(String question, String answer) {
    }
}
