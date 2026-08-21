package com.skala.day2.helpdesk.finance.tool;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.skala.day2.helpdesk.finance.repository.TransactionRepository;

@Component
public class TransactionTicketTools {

    private final TransactionRepository transactionRepository;

    private final Map<String, Ticket> tickets =
            new ConcurrentHashMap<>();

    private final AtomicInteger sequence =
            new AtomicInteger(2000);

    public TransactionTicketTools(
            TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public record Ticket(
            String id,
            String transactionId,
            String reason,
            String requestedBy,
            Instant requestedAt,
            String status) {
    }

    @Tool(description = """
            금융 거래의 오류 처리 요청 티켓을 생성한다.
            실제 거래 상태를 직접 변경하지 않고 PENDING 티켓만 생성한다.
            사용자가 명시적으로 오류 처리 요청을 원하는 경우에만 사용한다.
            사용자가 접근 권한을 가진 거래에 대해서만 생성할 수 있다.
            """)
    public String createTransactionTicket(
            @ToolParam(description = "거래 ID. 예: TX-123")
            String transactionId,

            @ToolParam(description = "오류 처리 요청 사유")
            String reason,

            ToolContext context) {

        String userId = String.valueOf(
                context.getContext().get("userId"));

        if (!transactionRepository.ownsTransaction(
                transactionId, userId)) {

            return "해당 거래를 찾을 수 없습니다.";
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
                "FIN-TK-" + sequence.incrementAndGet();

        tickets.put(
                ticketId,
                new Ticket(
                        ticketId,
                        transactionId,
                        safeReason,
                        userId,
                        Instant.now(),
                        "PENDING"));

        return """
                거래 오류 처리 티켓이 생성되었습니다.
                티켓번호: %s
                거래: %s
                상태: PENDING
                """
                .formatted(ticketId, transactionId);
    }
}
