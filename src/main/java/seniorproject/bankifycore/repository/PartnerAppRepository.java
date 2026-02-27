package seniorproject.bankifycore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import seniorproject.bankifycore.domain.PartnerApp;
import seniorproject.bankifycore.domain.enums.PartnerAppStatus;
import seniorproject.bankifycore.dto.partner.PartnerPendingResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartnerAppRepository extends JpaRepository<PartnerApp, UUID> {
    Optional<PartnerApp> findByApiKeyHashAndStatus(String apiKeyHash, PartnerAppStatus status);

    boolean existsByApiKeyHash(String apiKeyHash);

    Optional<PartnerApp> findByApiKeyHash(String apiKeyHash);

    List<PartnerApp> findByStatus(PartnerAppStatus status);


    @Query("""
    SELECT new seniorproject.bankifycore.dto.partner.PartnerPendingResponse(
        p.id,
        p.name,
        p.contactEmail,
        p.callbackUrl,
        p.status,
        p.createdAt
    )
    FROM PartnerApp p
    WHERE p.status = seniorproject.bankifycore.domain.enums.PartnerAppStatus.PENDING
    ORDER BY p.createdAt DESC
""")
    List<PartnerPendingResponse> findPendingPartnerApps();


}
