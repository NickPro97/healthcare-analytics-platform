package com.nikhil.assistant.rag;

public interface AnswerGenerator {
    String generate(String systemPrompt, String userPrompt);
}
