package seniorproject.bankifycore.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import seniorproject.bankifycore.domain.User;
import seniorproject.bankifycore.domain.enums.UserRole;
import seniorproject.bankifycore.domain.enums.UserStatus;
import seniorproject.bankifycore.repository.UserRepository;

@Profile("dev") // ✅ only runs in dev
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initAdminUser() {
        return args -> {
            String adminEmail = System.getenv().getOrDefault("BANKIFY_DEV_ADMIN_EMAIL", "admin@bankify.local");
            String adminPass  = System.getenv().getOrDefault("BANKIFY_DEV_ADMIN_PASSWORD", "admin123");

            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User admin = User.builder()
                        .email(adminEmail)
                        .passwordHash(passwordEncoder.encode(adminPass))
                        .role(UserRole.ADMIN)
                        .status(UserStatus.ACTIVE)
                        .build();
                userRepository.save(admin);

                // ✅ DON'T print password
                System.out.println("Created dev admin user: " + adminEmail);
            }
        };
    }
}