package com.company.hrpayroll.repository.settlement;

import com.company.hrpayroll.entity.settlement.SettlementAdvanceRecovery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementAdvanceRecoveryRepository extends JpaRepository<SettlementAdvanceRecovery, Long> {

    List<SettlementAdvanceRecovery> findBySettlementIdOrderByIdAsc(Long settlementId);
}
