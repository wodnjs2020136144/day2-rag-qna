package com.skala.day2.helpdesk.semiconductor.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

@Repository
public class EquipmentRepository {

    private final Map<String, Equipment> equipments = new ConcurrentHashMap<>();

    public EquipmentRepository() {
        equipments.put(
                "EQ-101",
                new Equipment(
                        "EQ-101",
                        "user1",
                        "INSPECTION",
                        68.2,
                        "KIM",
                        "ETCH-1"));

        equipments.put(
                "EQ-102",
                new Equipment(
                        "EQ-102",
                        "user1",
                        "RUNNING",
                        64.7,
                        "LEE",
                        "PHOTO-2"));

        // 다른 사용자의 장비 — 접근통제 테스트용
        equipments.put(
                "EQ-999",
                new Equipment(
                        "EQ-999",
                        "user2",
                        "MAINTENANCE",
                        71.3,
                        "PARK",
                        "CVD-1"));
    }

    public Optional<Equipment> findOwned(String equipmentId, String userId) {
        return Optional.ofNullable(equipments.get(equipmentId))
                .filter(equipment -> equipment.ownerId().equals(userId));
    }

    public boolean ownsEquipment(String equipmentId, String userId) {
        return findOwned(equipmentId, userId).isPresent();
    }

    public record Equipment(
            String id,
            String ownerId,
            String status,
            double temperatureC,
            String engineer,
            String processLine) {
    }
}
