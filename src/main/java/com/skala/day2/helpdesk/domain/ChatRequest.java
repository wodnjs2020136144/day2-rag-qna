package com.skala.day2.helpdesk.domain;

public record ChatRequest(
        HelpDeskDomain domain,
        String question,
        String userId,
        String sessionId
) {
}
