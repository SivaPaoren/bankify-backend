package seniorproject.bankifycore.web.v1;

import seniorproject.bankifycore.consants.ApiPaths;
import seniorproject.bankifycore.domain.User;
import seniorproject.bankifycore.domain.enums.UserStatus;
import seniorproject.bankifycore.dto.admin.LoginRequest;
import seniorproject.bankifycore.dto.admin.LoginResponse;
import seniorproject.bankifycore.repository.UserRepository;
import seniorproject.bankifycore.security.JwtTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping(ApiPaths.ADMIN+"/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final seniorproject.bankifycore.service.AuditService auditService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.email());

        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        User user = optionalUser.get();
        if (user.getStatus() != UserStatus.ACTIVE) {
            return ResponseEntity.status(403).body("User is not active");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String token = jwtTokenService.generateToken(user);

        // Audit Log
        auditService.log(
                "USER", user.getId().toString(),
                "USER_LOGIN",
                "User", user.getId().toString(),
                "email=" + user.getEmail());

        LoginResponse response = new LoginResponse(
                token,
                user.getEmail(),
                user.getRole().name()
        );

        return ResponseEntity.ok(response);
    }

}
