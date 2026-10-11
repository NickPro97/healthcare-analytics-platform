package com.nikhil.assistant.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Retrieval-augmented generation:
 *  1. retrieve the most relevant reference notes from ChromaDB,
 *  2. optionally add a summary of one patient's recent data,
 *  3. ask the LLM to answer ONLY from that context and cite the notes it used.
 */
@Service
public class AssistantService {

    public static final String DISCLAIMER =
            "Decision support only. Not a diagnosis. Always confirm with clinical assessment and local protocols.";

    static final String SYSTEM_PROMPT = """
            You are a clinical decision-support assistant inside a hospital analytics platform.
            Rules:
            - Answer ONLY from the CONTEXT and PATIENT DATA provided. If they do not contain the answer, say you do not know.
            - Never invent patient values, drug doses or diagnoses.
            - Cite the reference notes you used in square brackets, e.g. [news2.md].
            - Be concise: short paragraphs or bullet points a busy clinician can scan.
            - You support clinicians; you do not replace clinical judgement.
            """;

    private final KnowledgeRetriever retriever;
    private final AnswerGenerator generator;
    private final PatientContextClient patientContextClient;
    private final int topK;

    public AssistantService(KnowledgeRetriever retriever, AnswerGenerator generator,
                            PatientContextClient patientContextClient,
                            @Value("${assistant.top-k:4}") int topK) {
        this.retriever = retriever;
        this.generator = generator;
        this.patientContextClient = patientContextClient;
        this.topK = topK;
    }

    public record Answer(String answer, List<String> sources, boolean usedPatientData, String disclaimer) {
    }

    public Answer ask(String question, Long patientId, String authorizationHeader) {
        List<Snippet> snippets = retriever.retrieve(question, topK);

        Optional<String> patientData = patientId == null
                ? Optional.empty()
                : patientContextClient.summarize(patientId, authorizationHeader);

        String userPrompt = buildUserPrompt(question, snippets, patientData.orElse(null));
        String answer = generator.generate(SYSTEM_PROMPT, userPrompt);

        List<String> sources = snippets.stream().map(Snippet::source)
                .collect(Collectors.toCollection(LinkedHashSet::new)).stream().toList();
        return new Answer(answer, sources, patientData.isPresent(), DISCLAIMER);
    }

    static String buildUserPrompt(String question, List<Snippet> snippets, String patientData) {
        StringBuilder sb = new StringBuilder("CONTEXT:\n");
        if (snippets.isEmpty()) {
            sb.append("(no reference notes found)\n");
        }
        for (Snippet s : snippets) {
            sb.append("[").append(s.source()).append("] ").append(s.text()).append("\n---\n");
        }
        if (patientData != null) {
            sb.append("\nPATIENT DATA:\n").append(patientData).append('\n');
        }
        sb.append("\nQUESTION: ").append(question);
        return sb.toString();
    }
}
