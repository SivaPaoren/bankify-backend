package seniorproject.bankifycore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import seniorproject.bankifycore.domain.WebhookDelivery;
import seniorproject.bankifycore.domain.enums.WebhookDeliveryStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    List<WebhookDelivery> findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            List<WebhookDeliveryStatus> statuses,
            Instant now
    );
}
