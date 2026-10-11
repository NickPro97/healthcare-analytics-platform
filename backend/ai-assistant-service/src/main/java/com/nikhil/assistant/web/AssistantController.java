package com.nikhil.assistant.web;

import com.nikhil.assistant.rag.AssistantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    public record AskRequest(@NotBlank @Size(max = 1000) String question, Long patientId) {
    }

    @PostMapping("/ask")
    public AssistantService.Answer ask(@Valid @RequestBody AskRequest request,
                                       @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return assistantService.ask(request.question().trim(), request.patientId(), authorization);
    }
}
