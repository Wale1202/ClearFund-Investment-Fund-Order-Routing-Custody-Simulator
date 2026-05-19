package com.clearfund.repository;

import com.clearfund.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    Optional<Holding> findByAccountIdAndFundId(Long accountId, Long fundId);

    List<Holding> findByAccountId(Long accountId);
}
