package seniorproject.bankifycore.domain;

import jakarta.persistence.*;
import lombok.*;
import seniorproject.bankifycore.domain.base.Auditable;
import seniorproject.bankifycore.domain.enums.PartnerAppStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "partner_apps", indexes = {
        @Index(name = "idx_partner_apps_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerApp extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PartnerAppStatus status = PartnerAppStatus.PENDING;

    // store the hash of the api // nullable because pending have no key yet
    @Column(name = "api_key_hash", nullable = true, unique = true, length = 128)
    private String apiKeyHash;

    // optional metadata for review
    @Column(name = "contact_email", nullable = true, length = 180)
    private String contactEmail;

    @Column(name = "callback_url", nullable = true, length = 500)
    private String callbackUrl;

    @Column(name = "webhook_last_status_code")
    private Integer webhookLastStatusCode;

    @Column(name = "webhook_last_attempt_at")
    private Instant webhookLastAttemptAt;

    @Column(name = "webhook_last_success_at")
    private Instant webhookLastSuccessAt;

    @Column(name = "webhook_last_error", length = 255)
    private String webhookLastError;

    @Builder.Default
    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("0")
    private Integer webhookConsecutiveFailures = 0;
}
