package com.innercircle.sacco.ledger.listener;

import com.innercircle.sacco.common.event.LoanApplicationEvent;
import com.innercircle.sacco.common.event.MemberCreatedEvent;
import com.innercircle.sacco.ledger.service.AccountSetupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberAccountListener {

    private final AccountSetupService accountSetupService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleMemberCreated(MemberCreatedEvent event) {
        log.info("Creating member accounts for member {} ({})", event.memberId(), event.memberNumber());
        accountSetupService.createMemberAccounts(event.memberId(), event.memberNumber());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleLoanApplication(LoanApplicationEvent event) {
        log.info("Ensuring loan sub-account for member {}", event.memberId());
        accountSetupService.ensureLoanSubAccount(event.memberId());
    }
}
