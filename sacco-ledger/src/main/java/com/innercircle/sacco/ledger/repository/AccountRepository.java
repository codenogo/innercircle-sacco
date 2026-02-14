package com.innercircle.sacco.ledger.repository;

import com.innercircle.sacco.ledger.entity.Account;
import com.innercircle.sacco.ledger.entity.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccountCode(String accountCode);

    List<Account> findByAccountType(AccountType accountType);

    List<Account> findByActiveTrue();

    List<Account> findByAccountTypeAndActiveTrue(AccountType accountType);

    boolean existsByAccountCode(String accountCode);
}
