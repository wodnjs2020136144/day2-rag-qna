package com.skala.day2.helpdesk.domain;

public record RagChunk(
        HelpDeskDomain domain,
        String source,
        String title,
        String version,
        double score,
        String snippet
) {
}
