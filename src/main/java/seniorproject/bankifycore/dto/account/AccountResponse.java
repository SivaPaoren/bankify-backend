package seniorproject.bankifycore.dto.account;

import seniorproject.bankifycore.domain.enums.AccountStatus;
import seniorproject.bankifycore.domain.enums.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
                UUID id,
                UUID customerId,
                String customerName,
                UUID partnerAppId,
                String accountNumber,
                AccountType type,
                String currency,
                BigDecimal balance,
                AccountStatus status,
                Instant createdAt,
                Instant updatedAt) {
}
