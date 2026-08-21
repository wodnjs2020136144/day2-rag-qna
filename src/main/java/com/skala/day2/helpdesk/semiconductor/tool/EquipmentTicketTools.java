package com.skala.day2.helpdesk.semiconductor.tool;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.skala.day2.helpdesk.semiconductor.repository.EquipmentRepository;

@Component
public class EquipmentTicketTools {

    private final EquipmentRepository equipmentRepository;

    private final Map<String, Ticket> tickets =
            new ConcurrentHashMap<>();

    private final AtomicInteger sequence =
            new AtomicInteger(1000);

    public EquipmentTicketTools(
            EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    public record Ticket(
            String id,
            String equipmentId,
            String reason,
            String requestedBy,
            Instant requestedAt,
            String status) {
    }

    @Tool(description = """
            반도체 장비의 점검 요청 티켓을 생성한다.
            실제 장비 상태를 직접 변경하지 않고 PENDING 티켓만 생성한다.
            사용자가 명시적으로 점검 요청을 원하는 경우에만 사용한다.
            사용자가 접근 권한을 가진 장비에 대해서만 생성할 수 있다.
            """)
    public String createInspectionTicket(
            @ToolParam(description = "장비 ID. 예: EQ-101")
            String equipmentId,

            @ToolParam(description = "점검 요청 사유")
            String reason,

            ToolContext context) {

        String userId = String.valueOf(
                context.getContext().get("userId"));

        if (!equipmentRepository.ownsEquipment(
                equipmentId, userId)) {

            return "해당 장비를 찾을 수 없습니다.";
        }

        String userMessage = String.valueOf(
                context.getContext()
                        .getOrDefault("userMessage", ""));

        String safeReason =
                reason != null
                && !reason.isBlank()
                && userMessage.contains(reason)
                        ? reason
                        : "미제공";

        String ticketId =
                "SEM-TK-" + sequence.incrementAndGet();

        tickets.put(
                ticketId,
                new Ticket(
                        ticketId,
                        equipmentId,
                        safeReason,
                        userId,
                        Instant.now(),
                        "PENDING"));

        return """
                점검 요청 티켓이 생성되었습니다.
                티켓번호: %s
                장비: %s
                상태: PENDING
                """
                .formatted(ticketId, equipmentId);
    }
}
