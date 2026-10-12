package com.nikhil.assistant.rag;

/** One retrieved chunk of the knowledge base. */
public record Snippet(String source, String title, String text) {
}
