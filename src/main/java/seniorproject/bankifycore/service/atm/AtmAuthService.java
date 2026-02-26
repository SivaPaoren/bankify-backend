package seniorproject.bankifycore.service.atm;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seniorproject.bankifycore.domain.enums.AccountStatus;
import seniorproject.bankifycore.domain.enums.CustomerStatus;
import seniorproject.bankifycore.dto.atm.AtmLoginRequest;
import seniorproject.bankifycore.dto.atm.AtmLoginResponse;
import seniorproject.bankifycore.repository.AccountRepository;
import seniorproject.bankifycore.security.JwtTokenService;
import seniorproject.bankifycore.service.AuditService;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AtmAuthService {

    private final AccountRepository accountRepo;
    private final JwtTokenService jwtService; // existing JWT generator
    private final PasswordEncoder passwordEncoder; // BCryptPasswordEncoder bean
    private final AuditService auditService;

    public UUID currentAccountId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("ATM request is not authenticated");
        }
        if (!(auth.getPrincipal() instanceof UUID id)) {
            throw new IllegalStateException("ATM principal must be accountId");
        }
        return id;
    }

    @Transactional
    public AtmLoginResponse login(AtmLoginRequest req) {

        var acc = accountRepo.findByAccountNumber(req.accountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Account number not found"));

        // block partner accounts from ATM
        if (acc.getPartnerApp() != null) {
            throw new IllegalArgumentException("Partners are not allowed to use atm");
        }

        // lockout check (optional)
        if (acc.getPinLockedUntil() != null && acc.getPinLockedUntil().isAfter(Instant.now())) {
            throw new IllegalStateException("ATM locked. Try later.");
        }

        if (acc.getPinHash() == null || !passwordEncoder.matches(req.pin(), acc.getPinHash())) {
            acc.setPinFailedAttempts(acc.getPinFailedAttempts() + 1);
            if (acc.getPinFailedAttempts() >= 5) {
                acc.setPinLockedUntil(Instant.now().plusSeconds(300)); // 5 min
                acc.setPinFailedAttempts(0);
            }
            throw new IllegalArgumentException("Password is incorrect");
        }

        // success: reset counters
        acc.setPinFailedAttempts(0);
        acc.setPinLockedUntil(null);

        // block frozen account/customer at login
        if (acc.getStatus() == AccountStatus.FROZEN) {
            throw new IllegalStateException("Account is frozen");
        }
        if (acc.getCustomer() != null && acc.getCustomer().getStatus() == CustomerStatus.FROZEN) {
            throw new IllegalStateException("Customer is frozen");
        }

        // ✅ issue JWT scoped to ATM
        // Put accountId in token so /api/atm/me/** never needs accountId from frontend.
        String token = jwtService.generateAtmToken(acc.getId());

        auditService.log(
                "ATM", acc.getId().toString(),
                "ATM_LOGIN",
                "ACCOUNT", acc.getId().toString(),
                "ATM user logged in successfully"
        );

        return new AtmLoginResponse(token, acc.isPinChangeRequired());
    }

}
