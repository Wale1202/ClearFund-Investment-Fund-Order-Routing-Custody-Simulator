package com.clearfund.repository;

import com.clearfund.entity.Fund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FundRepository extends JpaRepository<Fund, Long> {

    Optional<Fund> findByFundCode(String fundCode);

    boolean existsByFundCode(String fundCode);
}
