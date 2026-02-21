package seniorproject.bankifycore.utils.rabbitmq;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TxCreatedEvent(
        String eventType,      // "TX_CREATED"
        String actorType,      // ATM / PARTNER / USER
        String actorId,
        UUID txId,
        String txType,
        BigDecimal amount,
        String reference,
        UUID fromAccountId,
        UUID toAccountId,
        Instant createdAt
) {
}
