package com.skala.day2.helpdesk.finance.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.skala.day2.helpdesk.finance.repository.TransactionRepository;

@Component
public class TransactionTools {

    private static final Logger log =
            LoggerFactory.getLogger(TransactionTools.class);

    private final TransactionRepository repository;

    public TransactionTools(TransactionRepository repository) {
        this.repository = repository;
    }

    @Tool(description = """
            금융 거래 ID로 현재 거래 상태를 조회한다.
            거래 처리 상태, 금액, 승인 상태를 확인할 때 사용한다.
            사용자가 접근할 수 있는 본인의 거래만 조회한다.
            """)
    public String transactionStatus(
            @ToolParam(description = "거래 ID. 예: TX-123")
            String transactionId,
            ToolContext context) {

        String userId = currentUser(context);

        log.info(
                "[FINANCE TOOL] transactionStatus transactionId={} by={}",
                transactionId,
                userId);

        return repository.findOwned(transactionId, userId)
                .map(transaction ->
                        """
                        거래 %s · 상태 %s · 금액 %,d원 · 승인 상태 %s
                        """
                        .formatted(
                                transaction.id(),
                                transaction.status(),
                                transaction.amount(),
                                transaction.approvalStatus())
                        .trim())
                .orElse("해당 거래를 찾을 수 없습니다.");
    }

    public boolean ownsTransaction(String transactionId, String userId) {
        return repository.ownsTransaction(transactionId, userId);
    }

    private String currentUser(ToolContext context) {
        Object userId =
                context == null
                        ? null
                        : context.getContext().get("userId");

        if (userId == null) {
            throw new IllegalStateException(
                    "toolContext에 userId가 없습니다.");
        }

        return userId.toString();
    }
}
