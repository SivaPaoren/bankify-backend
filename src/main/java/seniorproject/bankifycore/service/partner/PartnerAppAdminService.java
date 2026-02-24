package seniorproject.bankifycore.service.partner;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import seniorproject.bankifycore.domain.Account;
import seniorproject.bankifycore.domain.PartnerApp;
import seniorproject.bankifycore.domain.PartnerKeyRotationRequest;
import seniorproject.bankifycore.domain.enums.*;
import seniorproject.bankifycore.dto.admin.ApprovePartnerResponse;
import seniorproject.bankifycore.dto.partner.PartnerPendingResponse;
import seniorproject.bankifycore.dto.partnerapp.PartnerAppResponse;
import seniorproject.bankifycore.dto.rotation.ApproveRotationResponse;
import seniorproject.bankifycore.dto.rotation.RejectRotationResponse;
import seniorproject.bankifycore.dto.rotation.AdminRotationRequestItem;
import seniorproject.bankifycore.repository.AccountRepository;
import seniorproject.bankifycore.repository.PartnerAppRepository;
import seniorproject.bankifycore.repository.RotationRepository;
import seniorproject.bankifycore.service.AccountService;
import seniorproject.bankifycore.service.AuditService;
import seniorproject.bankifycore.utils.ActorContext;
import seniorproject.bankifycore.utils.ApiKeyUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartnerAppAdminService {

    private final PartnerAppRepository partnerAppRepo;
    private final AccountRepository accountRepo;
    private final AccountService accountService;
    private final RotationRepository rotationRepo;
    private final AuditService auditService;

    @Value("${security.api-key.pepper:change-me}")
    private String pepper;



    @Transactional
    public List<PartnerAppResponse> list() {
        return partnerAppRepo.findAll().stream()
                .map(this::toPartnerAppResponse)
                .toList();
    }

    @Transactional
    public PartnerAppResponse disable(UUID id) {
        PartnerApp partner = partnerAppRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PartnerApp not found"));

        partner.setStatus(PartnerAppStatus.DISABLED);
        partnerAppRepo.save(partner);
        // audit log is generated here
        auditService.log(
                ActorContext.actorType(), ActorContext.actorId(),
                "PARTNER_APP_DISABLED",
                "PartnerApp", partner.getId().toString(),
                "status=" + partner.getStatus().name());

        return toPartnerAppResponse(partner);
    }

    @Transactional
    public ApprovePartnerResponse approve(UUID partnerAppId) {
        PartnerApp app = partnerAppRepo.findById(partnerAppId)
                .orElseThrow(() -> new IllegalArgumentException("PartnerApp not found"));

        if (app.getStatus() != PartnerAppStatus.PENDING) {
            throw new IllegalStateException("PartnerApp is not pending");
        }

        if (app.getApiKeyHash() != null) {
            throw new IllegalStateException("API key already issued");
        }

        // generate key ONCE (store only hash)
        String apiKeyPlain, apiKeyHash;
        do {
            apiKeyPlain = ApiKeyUtils.generateRawKey();
            apiKeyHash = ApiKeyUtils.hash(apiKeyPlain, pepper);
        } while (partnerAppRepo.existsByApiKeyHash(apiKeyHash));

        app.setApiKeyHash(apiKeyHash);
        app.setStatus(PartnerAppStatus.ACTIVE);

        // create settlement account if missing
        if (!accountRepo.existsByPartnerApp_Id(app.getId())) {
            Account settlement = Account.builder()
                    .partnerApp(app)
                    .customer(null)
                    .type(AccountType.CURRENT)
                    .status(AccountStatus.ACTIVE)
                    .accountNumber(accountService.getUniqueAccountNumber())
                    .currency(Currency.THB) // or whatever default you want
                    .balance(BigDecimal.ZERO)
                    .build();
            accountRepo.save(settlement);
        }

        partnerAppRepo.save(app);

        // audit log is generated here
        auditService.log(
                ActorContext.actorType(), ActorContext.actorId(),
                "PARTNER_APP_APPROVED",
                "PartnerApp", app.getId().toString(),
                "status=" + app.getStatus().name());

        return new ApprovePartnerResponse(app.getId(), app.getStatus().name(), apiKeyPlain);
    }

    @Transactional(readOnly = true)
    public List<AdminRotationRequestItem> listRotationRequests() {
        return rotationRepo.findAll().stream()
                .filter(r -> r.getStatus() == RotationStatus.PENDING)
                .map(r -> new AdminRotationRequestItem(
                        r.getId(),
                        r.getPartnerApp().getId(),
                        r.getPartnerApp().getName(),
                        r.getStatus().name(),
                        r.getReason(),
                        r.getCreatedAt()))
                .toList();
    }

    // Here is the method that will approve , Rotation for partner
    @Transactional
    public ApproveRotationResponse approveRotation(UUID requestId) {
        PartnerKeyRotationRequest r = rotationRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Rotation request not found"));

        if (r.getStatus() != RotationStatus.PENDING) {
            throw new IllegalStateException("Rotation request is not pending");
        }

        PartnerApp app = r.getPartnerApp();
        if (app.getStatus() != PartnerAppStatus.ACTIVE) {
            throw new IllegalStateException("Partner app is not active");
        }

        // Generate new key (plain once), store hash
        String apiKeyPlain, apiKeyHash;
        do {
            apiKeyPlain = ApiKeyUtils.generateRawKey();
            apiKeyHash = ApiKeyUtils.hash(apiKeyPlain, pepper);
        } while (partnerAppRepo.existsByApiKeyHash(apiKeyHash));

        app.setApiKeyHash(apiKeyHash);
        partnerAppRepo.save(app);

        r.setStatus(RotationStatus.APPROVED);
        rotationRepo.save(r);

        auditService.log(
                ActorContext.actorType(), ActorContext.actorId(),
                "PARTNER_ROTATION_APPROVED",
                "PartnerApp", app.getId().toString(),
                "reason=admin_approve_rotation");

        return new ApproveRotationResponse(r.getId(), app.getId(), r.getStatus().name(), apiKeyPlain);
    }

    @Transactional
    public RejectRotationResponse reject(UUID requestId) {
        PartnerKeyRotationRequest r = rotationRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Rotation request not found"));

        if (r.getStatus() != RotationStatus.PENDING) {
            throw new IllegalStateException("Rotation request is not pending");
        }

        r.setStatus(RotationStatus.REJECTED);
        rotationRepo.save(r);

        auditService.log(
                ActorContext.actorType(), ActorContext.actorId(),
                "PARTNER_ROTATION_REJECTED",
                "PartnerApp", r.getPartnerApp().getId().toString(),
                "reason=admin_reject_rotation");

        return new RejectRotationResponse(r.getId(), r.getStatus().name());
    }



    public PartnerAppResponse activate(UUID id) {
        PartnerApp partner = partnerAppRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PartnerApp not found"));

        partner.setStatus(PartnerAppStatus.ACTIVE);
        partnerAppRepo.save(partner);
        // audit log is generated here
        auditService.log(
                ActorContext.actorType(), ActorContext.actorId(),
                "PARTNER_APP_ACTIVATED",
                "PartnerApp", partner.getId().toString(),
                "status=" + partner.getStatus().name());

        return toPartnerAppResponse(partner);
    }


    //return list of Parnter that are pending API keys, for the first time
    public List<PartnerPendingResponse> listPendingPartnerApps() {
        return partnerAppRepo.findPendingPartnerApps();
    }


    private PartnerAppResponse toPartnerAppResponse(PartnerApp partner) {
        return new PartnerAppResponse(
                partner.getId(),
                partner.getName(),
                partner.getStatus().name(),
                partner.getCreatedAt());
    }


}
