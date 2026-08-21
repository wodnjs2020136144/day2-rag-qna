package com.skala.day2.lab3.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skala.day2.lab3.tool.ApprovalTools;
import com.skala.day2.lab3.tool.ApprovalTools.Approval;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/lab3/admin/tickets")
@Tag(name = "Day 3 · Approval Admin")
public class Lab3AdminController {

    private final ApprovalTools approvalTools;

    public Lab3AdminController(ApprovalTools approvalTools) {
        this.approvalTools = approvalTools;
    }

    @GetMapping("/pending")
    @Operation(summary = "승인 대기 티켓 조회")
    public List<Approval> pending() {
        return approvalTools.pending();
    }

    @PostMapping("/approve")
    @Operation(summary = "담당자가 티켓 승인")
    public Approval approve(@RequestParam String id) {
        return approvalTools.approve(id);
    }
}
