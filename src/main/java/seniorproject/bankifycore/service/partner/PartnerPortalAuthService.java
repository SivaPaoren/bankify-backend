package seniorproject.bankifycore.service.partner;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seniorproject.bankifycore.domain.PartnerApp;
import seniorproject.bankifycore.domain.PartnerUser;
import seniorproject.bankifycore.domain.enums.PartnerUserRole;
import seniorproject.bankifycore.domain.enums.PartnerUserStatus;
import seniorproject.bankifycore.domain.enums.PartnerAppStatus;
import seniorproject.bankifycore.dto.partner.PartnerLoginRequest;
import seniorproject.bankifycore.dto.partner.PartnerLoginResponse;
import seniorproject.bankifycore.dto.partner.PartnerSignupRequest;
import seniorproject.bankifycore.dto.partner.PartnerSignupResponse;
import seniorproject.bankifycore.repository.PartnerUserRepository;
import seniorproject.bankifycore.repository.PartnerAppRepository;
import seniorproject.bankifycore.security.JwtTokenService;
import seniorproject.bankifycore.service.AuditService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartnerPortalAuthService {

    private final PartnerUserRepository partnerUserRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final PartnerAppRepository partnerAppRepo;
    private final AuditService auditService;

    // This method retrieves the UUID of the currently authenticated partner from
    // Spring Security’s security context and throws an error if the
    // request is unauthenticated.
    public UUID currentPartnerUserIdId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("Partner principal must be partnerId UUID");
        }
        return (UUID) auth.getPrincipal();
    }

    @Transactional(readOnly = true)
    public PartnerLoginResponse login(PartnerLoginRequest req) {
        String email = (req.email() == null) ? null : req.email().trim().toLowerCase();
        if (email == null || email.isBlank() || req.password() == null) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        PartnerUser user = partnerUserRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (user.getStatus() != PartnerUserStatus.ACTIVE) {
            throw new IllegalStateException("Partner user is disabled");
        }

        // ✅ (app status gate)
        var app = user.getPartnerApp();
        if (app.getStatus() == PartnerAppStatus.DISABLED || app.getStatus() == PartnerAppStatus.REJECTED) {
            throw new IllegalStateException("Partner app is disabled or rejected");
        }
        if (app.getStatus() == PartnerAppStatus.PENDING) {
            throw new IllegalStateException("Partner app is pending admin approval");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtTokenService.generatePartnerPortalToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        auditService.log(
                "PARTNER", user.getId().toString(),
                "PARTNER_LOGIN",
                "PARTNER_USER", user.getId().toString(),
                "Partner logged in successfully"
        );

        return new PartnerLoginResponse(token);
    }

    @Transactional
    public PartnerSignupResponse signup(PartnerSignupRequest req) {

        if (req.appName() == null || req.appName().isBlank()) {
            throw new IllegalArgumentException("appName is required");
        }
        if (req.email() == null || req.email().isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        if (req.password() == null || req.password().length() < 8) {
            throw new IllegalArgumentException("password must be at least 8 characters");
        }

        // Enforce global-unique email for portal login
        if (partnerUserRepo.existsByEmail(req.email().trim().toLowerCase())) {
            throw new IllegalArgumentException("email already registered");
        }

        PartnerApp app = PartnerApp.builder()
                .name(req.appName().trim())
                .status(PartnerAppStatus.PENDING)
                .apiKeyHash(null)
                .contactEmail(req.email().trim().toLowerCase())
                .callbackUrl(req.callbackUrl())
                .build();

        PartnerApp savedApp = partnerAppRepo.save(app);

        PartnerUser user = PartnerUser.builder()
                .email(req.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .status(PartnerUserStatus.ACTIVE)
                .role(PartnerUserRole.OWNER)
                .partnerApp(savedApp)
                .build();

        partnerUserRepo.save(user);

        return new PartnerSignupResponse(savedApp.getId(), savedApp.getStatus().name());
    }

}
