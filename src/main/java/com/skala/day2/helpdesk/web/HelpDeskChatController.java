package com.skala.day2.helpdesk.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skala.day2.helpdesk.domain.ChatRequest;
import com.skala.day2.helpdesk.domain.ChatResponse;
import com.skala.day2.helpdesk.service.HelpDeskService;

@RestController
@RequestMapping("/api/helpdesk")
public class HelpDeskChatController {

    private final HelpDeskService service;

    public HelpDeskChatController(HelpDeskService service) {
        this.service = service;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return service.chat(request);
    }
}
