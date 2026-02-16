package com.innercircle.sacco.ledger.service;

import java.util.UUID;

public interface AccountSetupService {

    void createMemberAccounts(UUID memberId, String memberNumber);

    void ensureLoanSubAccount(UUID memberId);
}
