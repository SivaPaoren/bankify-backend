package seniorproject.bankifycore.web.v1.partner;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartnerKeyVaultService {


    private final StringRedisTemplate redis;
    private static final Duration TTL = Duration.ofMinutes(5);

    public void store(UUID partnerAppId, String rawKey) {
        redis.opsForValue().set("partner:key:rotation:" + partnerAppId, rawKey, TTL);
    }

    public String retrieve(UUID partnerAppId) {
        String redisKey = "partner:key:rotation:" + partnerAppId;
        String key = redis.opsForValue().get(redisKey);
        if (key != null) redis.delete(redisKey); // one-time retrieval
        return key;
    }
}
