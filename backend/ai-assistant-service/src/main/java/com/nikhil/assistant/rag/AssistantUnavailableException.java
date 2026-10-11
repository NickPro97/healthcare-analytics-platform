package com.nikhil.assistant.rag;

public class AssistantUnavailableException extends RuntimeException {
    public AssistantUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
