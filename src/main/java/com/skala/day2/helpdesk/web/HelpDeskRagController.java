package com.skala.day2.helpdesk.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skala.day2.helpdesk.domain.HelpDeskDomain;
import com.skala.day2.helpdesk.domain.RagChunk;
import com.skala.day2.helpdesk.service.HelpDeskRagService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/helpdesk/rag")
@Tag(name = "HelpDesk RAG")
public class HelpDeskRagController {

    private final HelpDeskRagService ragService;

    public HelpDeskRagController(HelpDeskRagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ingest")
    @Operation(summary = "반도체·금융 규정 문서 인제스트")
    public List<RagChunk> ingest() {
        return ragService.ingestAll();
    }

    @GetMapping("/retrieve")
    @Operation(summary = "도메인별 RAG 검색 결과 확인")
    public List<RagChunk> retrieve(
            @RequestParam HelpDeskDomain domain,
            @RequestParam String q,
            @RequestParam(defaultValue = "4") int topK) {

        return ragService.retrieve(domain, q, topK);
    }
}
