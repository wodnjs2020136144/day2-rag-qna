package com.skala.day2.helpdesk.semiconductor.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.skala.day2.helpdesk.semiconductor.repository.EquipmentRepository;

@Component
public class EquipmentTools {

    private static final Logger log =
            LoggerFactory.getLogger(EquipmentTools.class);

    private final EquipmentRepository repository;

    public EquipmentTools(EquipmentRepository repository) {
        this.repository = repository;
    }

    @Tool(description = """
            반도체 장비 ID로 현재 장비 상태를 조회한다.
            장비 상태, 공정 라인, 온도, 담당 엔지니어 정보를 확인할 때 사용한다.
            사용자가 접근할 수 있는 본인의 장비만 조회한다.
            """)
    public String equipmentStatus(
            @ToolParam(description = "장비 ID. 예: EQ-101")
            String equipmentId,
            ToolContext context) {

        String userId = currentUser(context);

        log.info(
                "[SEMICONDUCTOR TOOL] equipmentStatus equipmentId={} by={}",
                equipmentId,
                userId);

        return repository.findOwned(equipmentId, userId)
                .map(equipment ->
                        """
                        장비 %s · 상태 %s · 공정 라인 %s · 온도 %.1f°C · 담당 엔지니어 %s
                        """
                        .formatted(
                                equipment.id(),
                                equipment.status(),
                                equipment.processLine(),
                                equipment.temperatureC(),
                                equipment.engineer())
                        .trim())
                .orElse("해당 장비를 찾을 수 없습니다.");
    }

    public boolean ownsEquipment(String equipmentId, String userId) {
        return repository.ownsEquipment(equipmentId, userId);
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
