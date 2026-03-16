package seniorproject.bankifycore.dto.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
                UUID id,
                UUID accountId,
                UUID transactionId,
                String direction,
                BigDecimal amount,
                String currency,
                Instant createdAt) {
}
