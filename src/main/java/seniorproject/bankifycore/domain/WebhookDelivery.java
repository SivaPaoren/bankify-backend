package seniorproject.bankifycore.domain;

import jakarta.persistence.*;
import lombok.*;
import seniorproject.bankifycore.domain.base.Auditable;
import seniorproject.bankifycore.domain.enums.WebhookDeliveryStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_deliveries", indexes = {
        @Index(name = "idx_webhook_deliveries_due", columnList = "status,nextAttemptAt"),
        @Index(name = "idx_webhook_deliveries_partner", columnList = "partner_app_id")
})
@Getter
@Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class WebhookDelivery extends Auditable {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_app_id")
    private PartnerApp partnerApp;

    @Column(nullable = false, length = 64)
    private String eventType; // "TX_CREATED"

    @Lob
    @Column(nullable = false)
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WebhookDeliveryStatus status;

    @Column(nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    private Integer lastStatusCode;

    @Column(length = 255)
    private String lastError;
}