package com.nikhil.assistant.rag;

import java.util.List;

public interface KnowledgeRetriever {
    List<Snippet> retrieve(String question, int topK);
}
