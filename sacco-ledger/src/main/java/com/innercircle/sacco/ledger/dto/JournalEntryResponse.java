package com.innercircle.sacco.ledger.dto;

import com.innercircle.sacco.ledger.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryResponse {

    private UUID id;
    private String entryNumber;
    private LocalDate transactionDate;
    private String description;
    private TransactionType transactionType;
    private UUID referenceId;
    private boolean posted;
    private Instant postedAt;
    private List<JournalLineDto> journalLines;
    private Instant createdAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JournalLineDto {
        private UUID id;
        private String accountCode;
        private String accountName;
        private String debitAmount;
        private String creditAmount;
        private String description;
    }
}
