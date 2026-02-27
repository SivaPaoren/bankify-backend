package seniorproject.bankifycore.dto;

import java.util.UUID;

public record RotationKeyResponse(
        UUID partnerAppId,
        String apiKey,
        String note
) {
}
