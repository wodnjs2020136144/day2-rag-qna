package com.skala.day2.helpdesk.domain;

public record ChatResponse(
        HelpDeskDomain domain,
        String answer,
        String conversationId
) {
}
