package com.nikhil.assistant;

import com.nikhil.assistant.rag.AnswerGenerator;
import com.nikhil.assistant.rag.AssistantService;
import com.nikhil.assistant.rag.KnowledgeRetriever;
import com.nikhil.assistant.rag.PatientContextClient;
import com.nikhil.assistant.rag.Snippet;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssistantServiceTest {

    private final KnowledgeRetriever retriever = mock(KnowledgeRetriever.class);
    private final AnswerGenerator generator = mock(AnswerGenerator.class);
    private final PatientContextClient patients = mock(PatientContextClient.class);
    private final AssistantService service = new AssistantService(retriever, generator, patients, 4);

    @Test
    void groundsAnswerInRetrievedNotesAndDeduplicatesSources() {
        when(retriever.retrieve("What is a red score?", 4)).thenReturn(List.of(
                new Snippet("news2.md", "NEWS2", "A score of 3 in any single parameter is a red score."),
                new Snippet("news2.md", "NEWS2", "Aggregate 5-6: medium risk."),
                new Snippet("platform-guide.md", "Guide", "Alerts show reasons.")));
        when(generator.generate(anyString(), anyString())).thenReturn("A red score is 3 points in one parameter [news2.md].");

        AssistantService.Answer answer = service.ask("What is a red score?", null, "Bearer t");

        assertThat(answer.answer()).contains("[news2.md]");
        assertThat(answer.sources()).containsExactly("news2.md", "platform-guide.md");
        assertThat(answer.usedPatientData()).isFalse();
        assertThat(answer.disclaimer()).contains("Decision support only");
        verify(patients, never()).summarize(org.mockito.ArgumentMatchers.any(), anyString());

        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(generator).generate(anyString(), userPrompt.capture());
        assertThat(userPrompt.getValue()).contains("[news2.md] A score of 3").contains("QUESTION: What is a red score?");
    }

    @Test
    void includesPatientDataWhenPatientSelected() {
        when(retriever.retrieve(anyString(), eq(4))).thenReturn(List.of());
        when(patients.summarize(7L, "Bearer t")).thenReturn(Optional.of("Patient #7, sex F\nHR 120"));
        when(generator.generate(anyString(), anyString())).thenReturn("ok");

        AssistantService.Answer answer = service.ask("How is this patient?", 7L, "Bearer t");

        assertThat(answer.usedPatientData()).isTrue();
        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(generator).generate(anyString(), userPrompt.capture());
        assertThat(userPrompt.getValue()).contains("PATIENT DATA:").contains("HR 120").contains("(no reference notes found)");
    }

    @Test
    void patientFormattingSkipsMissingValues() {
        String text = invokeFormat();
        assertThat(text).contains("Patient #3, sex M");
        assertThat(text).contains("ML in-hospital mortality risk (first 24h vitals): 42% (HIGH, model m1)");
        assertThat(text).contains("HR 110.0; ").doesNotContain("SBP");
    }

    private static String invokeFormat() {
        // format() is package-private in rag; call through a tiny reflection-free shim
        return com.nikhil.assistant.rag.FormatShim.format();
    }
}
