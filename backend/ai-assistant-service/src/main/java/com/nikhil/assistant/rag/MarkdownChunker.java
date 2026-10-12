package com.nikhil.assistant.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a markdown document at "## " headings, then splits long sections at paragraph
 * boundaries so each chunk stays under {@code maxChars}. Each chunk keeps its heading,
 * which makes retrieved text self-explanatory.
 */
public final class MarkdownChunker {

    private MarkdownChunker() {
    }

    public record Chunk(String title, String text) {
    }

    public static List<Chunk> chunk(String markdown, int maxChars) {
        List<Chunk> chunks = new ArrayList<>();
        String docTitle = "";
        String currentTitle = "";
        StringBuilder current = new StringBuilder();

        for (String line : markdown.split("\\R", -1)) {
            if (line.startsWith("# ") && docTitle.isEmpty()) {
                docTitle = line.substring(2).trim();
                currentTitle = docTitle;
                continue;
            }
            if (line.startsWith("## ")) {
                flush(chunks, currentTitle, current, maxChars);
                currentTitle = docTitle.isEmpty() ? line.substring(3).trim() : docTitle + " - " + line.substring(3).trim();
                continue;
            }
            current.append(line).append('\n');
        }
        flush(chunks, currentTitle, current, maxChars);
        return chunks;
    }

    private static void flush(List<Chunk> chunks, String title, StringBuilder body, int maxChars) {
        String text = body.toString().trim();
        body.setLength(0);
        if (text.isEmpty()) {
            return;
        }
        StringBuilder part = new StringBuilder();
        for (String paragraph : text.split("\\n\\s*\\n")) {
            if (part.length() > 0 && part.length() + paragraph.length() + 2 > maxChars) {
                chunks.add(new Chunk(title, part.toString().trim()));
                part.setLength(0);
            }
            if (paragraph.length() > maxChars) {
                // a single very long paragraph: hard split
                for (int i = 0; i < paragraph.length(); i += maxChars) {
                    chunks.add(new Chunk(title, paragraph.substring(i, Math.min(paragraph.length(), i + maxChars)).trim()));
                }
                continue;
            }
            part.append(paragraph).append("\n\n");
        }
        if (part.length() > 0) {
            chunks.add(new Chunk(title, part.toString().trim()));
        }
    }
}
