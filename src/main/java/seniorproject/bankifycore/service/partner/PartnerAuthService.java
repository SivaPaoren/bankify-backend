package seniorproject.bankifycore.service.partner;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import seniorproject.bankifycore.domain.PartnerApp;
import seniorproject.bankifycore.domain.enums.PartnerAppStatus;
import seniorproject.bankifycore.repository.PartnerAppRepository;
import seniorproject.bankifycore.utils.ApiKeyHasher;
import seniorproject.bankifycore.utils.ApiKeyUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartnerAuthService {

    private final PartnerAppRepository partnerAppRepository;
    private final ApiKeyHasher apiKeyHasher; // your existing hash util (pepper)

    public UUID authenticate(HttpServletRequest request) {

        String rawKey = request.getHeader("X-API-KEY");

        if (rawKey == null || rawKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing API key");
        }

        String hash = apiKeyHasher.hash(rawKey);

        PartnerApp app = partnerAppRepository.findByApiKeyHash(hash)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid API key"
                ));

        if (app.getStatus() != PartnerAppStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Partner app inactive"
            );
        }

        return app.getId();
    }
}



