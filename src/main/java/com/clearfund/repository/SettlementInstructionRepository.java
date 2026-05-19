package com.clearfund.repository;

import com.clearfund.entity.SettlementInstruction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementInstructionRepository
        extends JpaRepository<SettlementInstruction, Long> {

    Optional<SettlementInstruction> findByOrderId(Long orderId);
}
