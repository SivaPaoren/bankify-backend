package seniorproject.bankifycore.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyHasher {

    @Value("${security.api-key.pepper}")
    private String pepper;

    public String hash(String rawKey) {
        return ApiKeyUtils.hash(rawKey, pepper);
    }
}
