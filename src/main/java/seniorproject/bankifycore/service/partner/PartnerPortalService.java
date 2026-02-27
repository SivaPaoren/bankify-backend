package seniorproject.bankifycore.service.partner;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import seniorproject.bankifycore.domain.PartnerApp;
import seniorproject.bankifycore.domain.PartnerKeyRotationRequest;
import seniorproject.bankifycore.domain.PartnerUser;
import seniorproject.bankifycore.domain.enums.PartnerAppStatus;
import seniorproject.bankifycore.domain.enums.RotationStatus;
import seniorproject.bankifycore.dto.partner.PartnerPortalMeResponse;
import seniorproject.bankifycore.dto.rotation.RotateKeyRequest;
import seniorproject.bankifycore.dto.rotation.RotateKeyResponse;
import seniorproject.bankifycore.dto.rotation.RotationRequestItem;
import seniorproject.bankifycore.repository.PartnerAppRepository;
import seniorproject.bankifycore.repository.PartnerUserRepository;
import seniorproject.bankifycore.repository.RotationRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartnerPortalService {

        private final PartnerUserRepository partnerUserRepo;
        private final RotationRepository rotationRepo;


        private UUID currentPartnerUserId() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !(auth.getPrincipal() instanceof UUID id)) {
                        throw new IllegalStateException("Partner portal request is not authenticated");
                }
                return id;
        }

        @Transactional(readOnly = true)
        public PartnerPortalMeResponse me() {
                UUID userId = currentPartnerUserId();

                PartnerUser user = partnerUserRepo.findById(userId)
                                .orElseThrow(() -> new IllegalStateException("Partner user not found"));

                var app = user.getPartnerApp();

                // docs metadata (static list for now)
                List<PartnerPortalMeResponse.PartnerEndpointDoc> docs = List.of(
                                new PartnerPortalMeResponse.PartnerEndpointDoc("GET", "/api/partner/me/balance",
                                                "Get settlement balance", false),
                                new PartnerPortalMeResponse.PartnerEndpointDoc("GET",
                                                "/api/partner/me/transactions?limit=...",
                                                "Latest transactions", false),
                                new PartnerPortalMeResponse.PartnerEndpointDoc("POST", "/api/partner/me/deposit",
                                                "Credit settlement account", true),
                                new PartnerPortalMeResponse.PartnerEndpointDoc("POST", "/api/partner/me/withdraw",
                                                "Debit settlement account", true),
                                new PartnerPortalMeResponse.PartnerEndpointDoc("POST", "/api/partner/me/transfer",
                                                "Transfer to another account", true));

                return new PartnerPortalMeResponse(
                                user.getId(),
                                user.getEmail(),
                                user.getRole().name(),
                                app.getId(),
                                app.getName(),
                                app.getStatus().name(),
                                app.getApiKeyHash() != null,
                                docs);
        }

        // rotation happens here , rotaion of key
        @Transactional
        public RotateKeyResponse requestRotation(RotateKeyRequest req) {
                UUID partnerUserId = currentPartnerUserId();

                PartnerUser user = partnerUserRepo.findById(partnerUserId)
                                .orElseThrow(() -> new IllegalStateException("Partner user not found"));

                PartnerApp app = user.getPartnerApp();

                // Only ACTIVE apps can request rotation
                if (app.getStatus() != PartnerAppStatus.ACTIVE) {
                        throw new IllegalStateException("Partner app is not active");
                }

                // Prevent spam: only one pending request at a time
                if (rotationRepo.existsByPartnerApp_IdAndStatus(app.getId(), RotationStatus.PENDING)) {
                        throw new IllegalStateException("A rotation request is already pending");
                }

                PartnerKeyRotationRequest r = PartnerKeyRotationRequest.builder()
                                .partnerApp(app)
                                .requestedBy(user)
                                .status(RotationStatus.PENDING)
                                .reason(req == null ? null : req.reason())
                                .build();

                var saved = rotationRepo.save(r);
                return new RotateKeyResponse(saved.getId(), saved.getStatus().name());
        }

        @Transactional(readOnly = true)
        public List<RotationRequestItem> myRotationRequests() {
                UUID userId = currentPartnerUserId();

                PartnerUser user = partnerUserRepo.findById(userId)
                                .orElseThrow(() -> new IllegalStateException("Partner user not found"));

                UUID appId = user.getPartnerApp().getId();

                return rotationRepo.findByPartnerApp_IdOrderByCreatedAtDesc(appId).stream()
                                .map(r -> new RotationRequestItem(
                                                r.getId(),
                                                r.getStatus().name(),
                                                r.getReason(),
                                                r.getCreatedAt()))
                                .toList();
        }



        public UUID extractPartnerAppId(Authentication auth) {

                if (auth == null || auth.getPrincipal() == null) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
                }

                // your filter stores UUID directly as principal
                UUID partnerUserId;

                try {
                        partnerUserId = (UUID) auth.getPrincipal();
                } catch (ClassCastException e) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid partner authentication");
                }

                // now map user -> partner app
                return partnerUserRepo.findById(partnerUserId)
                        .map(user -> user.getPartnerApp().getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Partner user not found"));
        }

}
