package com.skala.day2.lab3.tool;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ApprovalTools {

    private static final Logger log = LoggerFactory.getLogger(ApprovalTools.class);

    private final OrderTools orderTools;

    private final Map<String, Approval> approvals = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(1000);

    public ApprovalTools(OrderTools orderTools) {
        this.orderTools = orderTools;
    }

    public record Approval(
            String id,
            String type,
            String targetId,
            String reason,
            String requestedBy,
            Instant requestedAt,
            String status) {}

    @Tool(description = """
            환불을 요청한다.
            실제 환불을 즉시 실행하지 않고 승인 대기 티켓만 생성한다.
            사용자가 명시적으로 환불을 요청한 경우에만 사용한다.
            """)
    public String requestRefund(
            @ToolParam(description = "주문번호") String orderId,
            @ToolParam(description = "환불 사유") String reason,
            ToolContext context) {

        String userId = String.valueOf(
                context.getContext().get("userId"));

        String userMessage = String.valueOf(
                context.getContext().getOrDefault("userMessage", ""));

        // 모델이 환불 사유를 임의로 만들어 내지 못하게 한다.
        String safeReason =
                reason != null
                && !reason.isBlank()
                && userMessage.contains(reason)
                        ? reason
                        : "미제공";

        // 남의 주문에는 환불 티켓조차 만들 수 없다.
        if (!orderTools.ownsOrder(orderId, userId)) {
            return "해당 주문을 찾을 수 없습니다.";
        }

        String id = "AP-" + sequence.incrementAndGet();

        approvals.put(id, new Approval(
                id,
                "REFUND",
                orderId,
                safeReason,
                userId,
                Instant.now(),
                "PENDING"));

        log.warn(
                "[APPROVAL] REFUND 요청 id={} order={} by={} reason={}",
                id, orderId, userId, safeReason);

        return "환불 요청 %s번으로 접수했습니다. 현재 상태는 PENDING이며 담당자 승인 후 처리됩니다."
                .formatted(id);
    }

    @Tool(description = "접수된 환불 요청의 처리 상태를 조회한다.")
    public String approvalStatus(
            @ToolParam(description = "요청 번호(예: AP-1001)") String approvalId) {

        Approval approval = approvals.get(approvalId);

        return approval == null
                ? "해당 요청 번호를 찾을 수 없습니다."
                : "요청 %s · 유형 %s · 상태 %s"
                        .formatted(
                                approval.id(),
                                approval.type(),
                                approval.status());
    }

    public List<Approval> pending() {
        return approvals.values().stream()
                .filter(a -> "PENDING".equals(a.status()))
                .toList();
    }

    public Approval approve(String id) {
        return approvals.computeIfPresent(id,
                (key, a) -> new Approval(
                        a.id(),
                        a.type(),
                        a.targetId(),
                        a.reason(),
                        a.requestedBy(),
                        a.requestedAt(),
                        "APPROVED"));
    }
}
