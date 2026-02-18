package com.innercircle.sacco.security.repository;

import com.innercircle.sacco.security.entity.UserAccount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findByEmail(String email);
    Optional<UserAccount> findByMemberId(UUID memberId);

    List<UserAccount> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username, String email, Pageable pageable);

    List<UserAccount> findByEnabledTrue(Pageable pageable);

    List<UserAccount> findByEnabledFalse(Pageable pageable);

    List<UserAccount> findByIdGreaterThanOrderById(UUID cursor, Pageable pageable);
}
