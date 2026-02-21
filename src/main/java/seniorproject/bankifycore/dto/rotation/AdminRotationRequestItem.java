package seniorproject.bankifycore.dto.rotation;

import java.time.Instant;
import java.util.UUID;

public record AdminRotationRequestItem(
        UUID id,
        UUID partnerId,
        String partnerName,
        String status,
        String reason,
        Instant requestedAt) {
}
