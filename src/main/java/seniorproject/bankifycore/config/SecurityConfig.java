package seniorproject.bankifycore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import seniorproject.bankifycore.consants.ApiPaths;
import seniorproject.bankifycore.repository.PartnerAppRepository;
import seniorproject.bankifycore.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import seniorproject.bankifycore.security.RateLimitService;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final PartnerAppRepository partnerAppRepository;
        private final JwtTokenService jwtTokenService; // for portal jwt filter too
        private final RateLimitService rateLimitService;

        @Value("${security.api-key.pepper:change-me}")
        private String apiKeyPepper;

        @Value("${security.ratelimit.partner.perMinute:60}")
        private long partnerLimitPerMin;

        @Value("${security.ratelimit.atm.perMinute:30}")
        private long atmLimitPerMin;

        // 1) Partner AUTH (public): signup + login
        @Bean
        @Order(1)
        public SecurityFilterChain partnerAuthChain(HttpSecurity http) throws Exception {
                http.securityMatcher("/api/v1/partner/auth/**")
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
                return http.build();
        }

        // 2) Partner API (server-to-server): X-API-Key only
        @Bean
        @Order(2)
        public SecurityFilterChain partnerApiChain(HttpSecurity http) throws Exception {
                http.securityMatcher("/api/v1/partner/me/**")
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("PARTNER"))
                                .addFilterBefore(
                                                new ApiKeyAuthenticationFilter(partnerAppRepository, apiKeyPepper),
                                                UsernamePasswordAuthenticationFilter.class)
                                // rate limit AFTER api-key auth so principal (partnerAppId) exists
                                .addFilterAfter(
                                                new RateLimitFilter(rateLimitService, partnerLimitPerMin, "partner"),
                                                ApiKeyAuthenticationFilter.class);
                return http.build();
        }

        // 3) Partner PORTAL (human): PARTNER_PORTAL JWT only
        @Bean
        @Order(3)
        public SecurityFilterChain partnerPortalChain(HttpSecurity http) throws Exception {
                http.securityMatcher("/api/v1/partner/portal/**")
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("PARTNER"))
                                .addFilterBefore(
                                                new PartnerPortalJwtAuthenticationFilter(jwtTokenService),
                                                UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }

        // 4) ATM chain
        @Bean
        @Order(4)
        public SecurityFilterChain atmChain(HttpSecurity http) throws Exception {
                http.securityMatcher("/api/v1/atm/**")
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/v1/atm/auth/login").permitAll()
                                                .anyRequest().hasRole("ATM"))
                                .addFilterBefore(new AtmJwtAuthenticationFilter(jwtTokenService),
                                                UsernamePasswordAuthenticationFilter.class)

                                // rate limit AFTER atm auth so principal exists
                                .addFilterAfter(
                                                new RateLimitFilter(rateLimitService, atmLimitPerMin, "atm"),
                                                AtmJwtAuthenticationFilter.class);
                return http.build();
        }

        // 5) Admin/Human chain: everything else
        @Bean
        @Order(5)
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http.csrf(csrf -> csrf.disable())
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                        "/error",
                                                        "/health",
                                                        ApiPaths.ADMIN + "/auth/login",
                                                        ApiPaths.API_V1 + "/bootstrap/**"
                                                        )
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
                return configuration.getAuthenticationManager();
        }
}
