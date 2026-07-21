package org.kimwanyi.sacco.dto.savings;

import lombok.Data;
import org.kimwanyi.sacco.enums.AccountStatus;
import org.kimwanyi.sacco.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SavingsResponse {
    private Long id;
    private String accountNumber;
    private Long memberId;
    private String memberName;
    private AccountStatus status;
    private BigDecimal balance;
    private List<TransactionDto> recentTransactions;

    @Data
    public static class TransactionDto {
        private Long id;
        private TransactionType type;
        private BigDecimal amount;
        private String description;
        private String referenceNumber;
        private LocalDateTime createdAt;
    }
}
