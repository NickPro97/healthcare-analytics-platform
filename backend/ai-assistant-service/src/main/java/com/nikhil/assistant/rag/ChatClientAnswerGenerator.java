package com.nikhil.assistant.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class ChatClientAnswerGenerator implements AnswerGenerator {

    private final ChatClient chatClient;

    public ChatClientAnswerGenerator(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (RuntimeException e) {
            throw new AssistantUnavailableException("Language model call failed: " + e.getMessage(), e);
        }
    }
}
