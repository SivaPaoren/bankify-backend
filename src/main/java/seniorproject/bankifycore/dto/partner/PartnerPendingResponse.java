package seniorproject.bankifycore.dto.partner;

import seniorproject.bankifycore.domain.enums.PartnerAppStatus;

import java.time.Instant;
import java.util.UUID;

public record PartnerPendingResponse(
        UUID partnerAppId,
        String name,
        String email,
        String callbackUrl,
        PartnerAppStatus status,
        Instant createdAt
) {
}

