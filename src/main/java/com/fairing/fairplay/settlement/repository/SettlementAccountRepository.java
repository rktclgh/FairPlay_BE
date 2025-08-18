package com.fairing.fairplay.settlement.repository;

import com.fairing.fairplay.settlement.entitiy.SettlementAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementAccountRepository extends JpaRepository<SettlementAccount, Long> {
    Optional<SettlementAccount> findBySettlement_SettlementId(Long settlementId);
}
