package com.nikhil.assistant.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Loads the curated clinical reference notes (src/main/resources/knowledge/*.md) into ChromaDB
 * once, at startup. Ids are deterministic, so a re-run does not create duplicates.
 * ChromaDB must be reachable when the service starts (Spring AI connects while creating the
 * vector store), so Docker Compose and Kubernetes start Chroma first and restart this service
 * until it is up. Embedding failures here (for example no OpenAI key) are only logged.
 */
@Component
public class KnowledgeBaseLoader {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseLoader.class);
    static final int MAX_CHARS = 1200;

    private final VectorStore vectorStore;
    private final boolean enabled;

    public KnowledgeBaseLoader(VectorStore vectorStore,
                               @Value("${assistant.ingest-on-startup:true}") boolean enabled) {
        this.vectorStore = vectorStore;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadOnStartup() {
        if (!enabled) {
            return;
        }
        try {
            if (!vectorStore.similaritySearch(SearchRequest.builder().query("NEWS2 early warning score").topK(1).build()).isEmpty()) {
                log.info("Knowledge base already loaded - skipping ingestion");
                return;
            }
            List<Document> docs = readDocuments();
            vectorStore.add(docs);
            log.info("Loaded {} knowledge chunks into the vector store", docs.size());
        } catch (Exception e) {
            log.warn("Knowledge base not loaded ({}). The assistant will answer once OpenAI and Chroma are reachable "
                    + "and the service is restarted.", e.getMessage());
        }
    }

    static List<Document> readDocuments() throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath:knowledge/*.md");
        List<Document> docs = new ArrayList<>();
        for (Resource resource : resources) {
            String source = resource.getFilename();
            String markdown = resource.getContentAsString(StandardCharsets.UTF_8);
            List<MarkdownChunker.Chunk> chunks = MarkdownChunker.chunk(markdown, MAX_CHARS);
            for (int i = 0; i < chunks.size(); i++) {
                MarkdownChunker.Chunk c = chunks.get(i);
                String id = UUID.nameUUIDFromBytes((source + "#" + i).getBytes(StandardCharsets.UTF_8)).toString();
                docs.add(Document.builder()
                        .id(id)
                        .text(c.title() + "\n\n" + c.text())
                        .metadata(Map.of("source", source, "title", c.title(), "chunk", i))
                        .build());
            }
        }
        return docs;
    }
}
