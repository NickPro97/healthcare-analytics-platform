package com.nikhil.assistant.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownChunkerTest {

    @Test
    void splitsAtSecondLevelHeadingsAndKeepsTitles() {
        String md = "# Doc\n\nIntro text.\n\n## Part A\nAlpha.\n\n## Part B\nBeta.\n";
        List<MarkdownChunker.Chunk> chunks = MarkdownChunker.chunk(md, 1000);
        assertThat(chunks).extracting(MarkdownChunker.Chunk::title)
                .containsExactly("Doc", "Doc - Part A", "Doc - Part B");
        assertThat(chunks.get(1).text()).isEqualTo("Alpha.");
    }

    @Test
    void longSectionsAreSplitUnderLimit() {
        String para = "x".repeat(300);
        String md = "# D\n## S\n" + para + "\n\n" + para + "\n\n" + para + "\n";
        List<MarkdownChunker.Chunk> chunks = MarkdownChunker.chunk(md, 700);
        assertThat(chunks).hasSize(2);
        assertThat(chunks).allSatisfy(c -> assertThat(c.text().length()).isLessThanOrEqualTo(700));
    }

    @Test
    void bundledKnowledgeBaseLoadsIntoDocuments() throws Exception {
        var docs = KnowledgeBaseLoader.readDocuments();
        assertThat(docs).hasSizeGreaterThan(10);
        assertThat(docs).allSatisfy(d -> {
            assertThat(d.getMetadata()).containsKey("source");
            assertThat(d.getText().length()).isLessThanOrEqualTo(KnowledgeBaseLoader.MAX_CHARS + 200);
        });
        // ids are deterministic so restarts do not duplicate chunks
        assertThat(KnowledgeBaseLoader.readDocuments().get(0).getId()).isEqualTo(docs.get(0).getId());
    }
}
