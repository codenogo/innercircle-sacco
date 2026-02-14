package com.innercircle.sacco.reporting.service;

import com.innercircle.sacco.reporting.dto.MemberStatementResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface MemberStatementService {

    MemberStatementResponse generateStatement(UUID memberId, LocalDate fromDate, LocalDate toDate);
}
