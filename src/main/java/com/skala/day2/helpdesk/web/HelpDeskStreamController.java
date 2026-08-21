package com.skala.day2.helpdesk.web;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skala.day2.helpdesk.domain.ChatRequest;
import com.skala.day2.helpdesk.service.HelpDeskStreamingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/helpdesk")
@Tag(name = "HelpDesk Streaming")
public class HelpDeskStreamController {

    private final HelpDeskStreamingService streamingService;

    public HelpDeskStreamController(
            HelpDeskStreamingService streamingService) {

        this.streamingService = streamingService;
    }

    @PostMapping(
            value = "/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "HelpDesk SSE 스트리밍 응답")
    public Flux<ServerSentEvent<String>> stream(
            @RequestBody ChatRequest request) {

        return streamingService.stream(request)
                .map(chunk ->
                        ServerSentEvent.<String>builder()
                                .event("message")
                                .data(chunk)
                                .build())
                .concatWith(
                        Flux.just(
                                ServerSentEvent.<String>builder()
                                        .event("done")
                                        .data("[DONE]")
                                        .build()));
    }
}
