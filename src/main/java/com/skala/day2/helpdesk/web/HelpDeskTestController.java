package com.skala.day2.helpdesk.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skala.day2.helpdesk.finance.repository.TransactionRepository;
import com.skala.day2.helpdesk.semiconductor.repository.EquipmentRepository;

@RestController
@RequestMapping("/api/helpdesk/test")
public class HelpDeskTestController {

    private final EquipmentRepository equipmentRepository;
    private final TransactionRepository transactionRepository;

    public HelpDeskTestController(
            EquipmentRepository equipmentRepository,
            TransactionRepository transactionRepository) {

        this.equipmentRepository = equipmentRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/semiconductor/{equipmentId}")
    public ResponseEntity<?> semiconductor(
            @PathVariable String equipmentId,
            @RequestParam String userId) {

        return equipmentRepository.findOwned(equipmentId, userId)
                .<ResponseEntity<?>>map(equipment ->
                        ResponseEntity.ok(Map.of(
                                "equipmentId", equipment.id(),
                                "status", equipment.status(),
                                "processLine", equipment.processLine(),
                                "temperatureC", equipment.temperatureC(),
                                "engineer", equipment.engineer())))
                .orElseGet(() ->
                        ResponseEntity.status(404).body(Map.of(
                                "message", "해당 장비를 찾을 수 없습니다.")));
    }

    @GetMapping("/finance/{transactionId}")
    public ResponseEntity<?> finance(
            @PathVariable String transactionId,
            @RequestParam String userId) {

        return transactionRepository.findOwned(transactionId, userId)
                .<ResponseEntity<?>>map(transaction ->
                        ResponseEntity.ok(Map.of(
                                "transactionId", transaction.id(),
                                "status", transaction.status(),
                                "amount", transaction.amount(),
                                "approvalStatus", transaction.approvalStatus())))
                .orElseGet(() ->
                        ResponseEntity.status(404).body(Map.of(
                                "message", "해당 거래를 찾을 수 없습니다.")));
    }
}
