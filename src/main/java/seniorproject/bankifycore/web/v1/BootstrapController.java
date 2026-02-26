package seniorproject.bankifycore.web.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import seniorproject.bankifycore.consants.ApiPaths;
import seniorproject.bankifycore.domain.User;
import seniorproject.bankifycore.domain.enums.UserRole;
import seniorproject.bankifycore.domain.enums.UserStatus;
import seniorproject.bankifycore.repository.UserRepository;

@RestController
@RequestMapping(ApiPaths.API_V1+"/bootstrap")
@RequiredArgsConstructor
public class BootstrapController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public record BootstrapAdminRequest(String email, String password) {}

    @PostMapping("/admin")
    public String createFirstAdmin(
            @RequestHeader(name = "X-BOOTSTRAP-TOKEN", required = false) String token,
            @RequestBody BootstrapAdminRequest req
    ) {
        String expected = System.getenv("BANKIFY_BOOTSTRAP_TOKEN");
        if (expected == null || expected.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bootstrap is disabled");
        }
        if (token == null || !constantTimeEquals(token, expected)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bootstrap token");
        }

        // ✅ Only allow if NO admin exists yet (one-time)
        boolean adminExists = userRepository.existsByRole(UserRole.ADMIN);
        if (adminExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin already exists");
        }

        if (req.email() == null || req.email().isBlank() || req.password() == null || req.password().length() < 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email required and password must be >= 10 chars");
        }

        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        User admin = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE) // even better: FORCE_PASSWORD_CHANGE
                .build();

        userRepository.save(admin);
        return "OK: First admin created";
    }

    // prevents timing attacks (simple)
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}