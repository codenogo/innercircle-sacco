package com.innercircle.sacco.ledger.service;

import com.innercircle.sacco.ledger.entity.Account;
import com.innercircle.sacco.ledger.entity.AccountType;
import com.innercircle.sacco.ledger.repository.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountSetupServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountSetupServiceImpl accountSetupService;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    @Nested
    @DisplayName("createMemberAccounts()")
    class CreateMemberAccounts {

        @Test
        @DisplayName("should create shares and savings accounts with correct codes and types")
        void shouldCreateTwoAccountsWithCorrectDetails() {
            UUID memberId = UUID.randomUUID();
            String memberNumber = "M001";

            when(accountRepository.existsByMemberIdAndParentAccountCode(memberId, "2001")).thenReturn(false);
            when(accountRepository.existsByMemberIdAndParentAccountCode(memberId, "2002")).thenReturn(false);

            accountSetupService.createMemberAccounts(memberId, memberNumber);

            verify(accountRepository, times(2)).save(accountCaptor.capture());
            List<Account> savedAccounts = accountCaptor.getAllValues();

            Account shares = savedAccounts.get(0);
            assertThat(shares.getAccountCode()).isEqualTo("2001-M001");
            assertThat(shares.getAccountType()).isEqualTo(AccountType.LIABILITY);
            assertThat(shares.getParentAccountCode()).isEqualTo("2001");
            assertThat(shares.getMemberId()).isEqualTo(memberId);

            Account savings = savedAccounts.get(1);
            assertThat(savings.getAccountCode()).isEqualTo("2002-M001");
            assertThat(savings.getAccountType()).isEqualTo(AccountType.LIABILITY);
            assertThat(savings.getParentAccountCode()).isEqualTo("2002");
            assertThat(savings.getMemberId()).isEqualTo(memberId);
        }

        @Test
        @DisplayName("should skip creation when accounts already exist (idempotent)")
        void shouldSkipWhenAccountsAlreadyExist() {
            UUID memberId = UUID.randomUUID();

            when(accountRepository.existsByMemberIdAndParentAccountCode(memberId, "2001")).thenReturn(true);
            when(accountRepository.existsByMemberIdAndParentAccountCode(memberId, "2002")).thenReturn(true);

            accountSetupService.createMemberAccounts(memberId, "M001");

            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("ensureLoanSubAccount()")
    class EnsureLoanSubAccount {

        @Test
        @DisplayName("should create loan sub-account when member has existing accounts but no loan account")
        void shouldCreateLoanAccountWhenNotExists() {
            UUID memberId = UUID.randomUUID();
            Account existingAccount = new Account();
            existingAccount.setAccountCode("2001-M001");
            existingAccount.setMemberId(memberId);

            when(accountRepository.findByMemberId(memberId)).thenReturn(List.of(existingAccount));
            when(accountRepository.existsByMemberIdAndParentAccountCode(memberId, "1002")).thenReturn(false);

            accountSetupService.ensureLoanSubAccount(memberId);

            verify(accountRepository).save(accountCaptor.capture());
            Account loanAccount = accountCaptor.getValue();
            assertThat(loanAccount.getAccountCode()).isEqualTo("1002-M001");
            assertThat(loanAccount.getAccountType()).isEqualTo(AccountType.ASSET);
            assertThat(loanAccount.getParentAccountCode()).isEqualTo("1002");
            assertThat(loanAccount.getMemberId()).isEqualTo(memberId);
        }

        @Test
        @DisplayName("should skip when loan account already exists")
        void shouldSkipWhenLoanAccountExists() {
            UUID memberId = UUID.randomUUID();
            Account existingAccount = new Account();
            existingAccount.setAccountCode("2001-M001");
            existingAccount.setMemberId(memberId);

            when(accountRepository.findByMemberId(memberId)).thenReturn(List.of(existingAccount));
            when(accountRepository.existsByMemberIdAndParentAccountCode(memberId, "1002")).thenReturn(true);

            accountSetupService.ensureLoanSubAccount(memberId);

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when no existing member accounts found")
        void shouldThrowWhenNoExistingAccounts() {
            UUID memberId = UUID.randomUUID();

            when(accountRepository.findByMemberId(memberId)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> accountSetupService.ensureLoanSubAccount(memberId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No existing accounts found for member");
        }
    }
}
