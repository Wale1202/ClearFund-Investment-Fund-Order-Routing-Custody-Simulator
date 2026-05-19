package com.clearfund.repository;

import com.clearfund.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountRef(String accountRef);

    boolean existsByEmail(String email);
}
