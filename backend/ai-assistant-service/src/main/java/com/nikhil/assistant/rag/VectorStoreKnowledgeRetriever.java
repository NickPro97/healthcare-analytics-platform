package com.nikhil.assistant.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

/** Similarity search in ChromaDB (embeddings from OpenAI) through Spring AI's VectorStore. */
@Component
public class VectorStoreKnowledgeRetriever implements KnowledgeRetriever {

    private final VectorStore vectorStore;

    public VectorStoreKnowledgeRetriever(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<Snippet> retrieve(String question, int topK) {
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder().query(question).topK(topK).build());
            return docs.stream()
                    .map(d -> new Snippet(
                            String.valueOf(d.getMetadata().getOrDefault("source", "unknown")),
                            String.valueOf(d.getMetadata().getOrDefault("title", "")),
                            d.getText()))
                    .toList();
        } catch (RuntimeException e) {
            throw new AssistantUnavailableException("Knowledge base search failed: " + e.getMessage(), e);
        }
    }
}
