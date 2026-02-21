package seniorproject.bankifycore.dto;

import jakarta.persistence.Column;
import java.time.Instant;

public record AuditLogResponse(
        Instant createdAt,        // When the event happened
        String actorType,         // USER / ATM / PARTNER
        String actorId,           // email / UUID of the actor
        String action,            // e.g. ACCOUNT_UPDATED, PIN_RESET
        String entityType,        // e.g. Account, Transaction
        String entityId,
        String details            // e.g reason=admin_FROZEN
) {
}
