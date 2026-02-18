package com.innercircle.sacco.ledger.listener;

import com.innercircle.sacco.common.event.LoanApplicationEvent;
import com.innercircle.sacco.common.event.MemberCreatedEvent;
import com.innercircle.sacco.ledger.service.AccountSetupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberAccountListenerTest {

    @Mock
    private AccountSetupService accountSetupService;

    @InjectMocks
    private MemberAccountListener memberAccountListener;

    @Test
    @DisplayName("handleMemberCreated should call createMemberAccounts with correct arguments")
    void handleMemberCreatedShouldCallCreateMemberAccounts() {
        UUID memberId = UUID.randomUUID();
        String memberNumber = "M001";
        MemberCreatedEvent event = new MemberCreatedEvent(memberId, memberNumber, "John", "Doe", UUID.randomUUID(), "system");

        memberAccountListener.handleMemberCreated(event);

        verify(accountSetupService).createMemberAccounts(memberId, memberNumber);
    }

    @Test
    @DisplayName("handleLoanApplication should call ensureLoanSubAccount with correct memberId")
    void handleLoanApplicationShouldCallEnsureLoanSubAccount() {
        UUID memberId = UUID.randomUUID();
        UUID loanId = UUID.randomUUID();
        LoanApplicationEvent event = new LoanApplicationEvent(loanId, memberId, "APPLY", UUID.randomUUID(), "system");

        memberAccountListener.handleLoanApplication(event);

        verify(accountSetupService).ensureLoanSubAccount(memberId);
    }
}
