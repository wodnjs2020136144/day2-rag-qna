package com.skala.day2.helpdesk.domain;

import java.util.List;

public record ChatResponse(
        HelpDeskDomain domain,
        String answer,
        String conversationId,
        List<SourceInfo> sources
) {
}
