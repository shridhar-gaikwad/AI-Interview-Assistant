package io.interviewAssistant.ai.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads source documents, splits them into chunks, and stores them (as embedding
 * vectors) in the pgvector-backed {@link VectorStore}. This is the "R" prep work
 * of RAG: without ingested documents there is nothing to retrieve.
 *
 * Flow: read files -> split into token-sized chunks -> VectorStore.add(), which
 * calls the embedding model for each chunk and writes the vectors to Postgres.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final VectorStore vectorStore;
    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    /** Location pattern (e.g. classpath:/docs/*) of documents to ingest. */
    private final String docsLocation;

    public IngestionService(VectorStore vectorStore,
                            @Value("${app.rag.docs-location}") String docsLocation) {
        this.vectorStore = vectorStore;
        this.docsLocation = docsLocation;
    }

    /**
     * Ingest every document matched by {@code app.rag.docs-location}.
     * Returns the number of chunks written to the vector store.
     *
     * NOTE: this performs blocking JDBC + embedding calls, so callers on a
     * reactive thread should offload it (see RagController).
     */
    public int ingestFromConfiguredLocation() {
        Resource[] resources = resolveResources();
        if (resources.length == 0) {
            log.warn("No documents found at '{}' - nothing to ingest.", docsLocation);
            return 0;
        }

        // Split large documents into ~overlapping chunks so retrieval returns
        // focused, relevant passages instead of whole files.
        TokenTextSplitter splitter = new TokenTextSplitter();

        List<Document> allChunks = new ArrayList<>();
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            TextReader reader = new TextReader(resource);
            // Tag each chunk with its source file so answers can be traced back.
            reader.getCustomMetadata().put("source", filename);

            List<Document> chunks = splitter.apply(reader.get());
            allChunks.addAll(chunks);
            log.info("Read '{}' -> {} chunk(s).", filename, chunks.size());
        }

        // add() embeds each chunk and persists it to the pgvector table.
        vectorStore.add(allChunks);
        log.info("Ingestion complete: {} chunk(s) stored in the vector store.", allChunks.size());
        return allChunks.size();
    }

    private Resource[] resolveResources() {
        try {
            return resourcePatternResolver.getResources(docsLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to resolve documents at " + docsLocation, e);
        }
    }
}
