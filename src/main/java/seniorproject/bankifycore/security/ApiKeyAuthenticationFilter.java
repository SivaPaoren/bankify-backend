package seniorproject.bankifycore.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import seniorproject.bankifycore.consants.ApiPaths;
import seniorproject.bankifycore.domain.enums.PartnerAppStatus;
import seniorproject.bankifycore.repository.PartnerAppRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final PartnerAppRepository partnerAppRepository;
    private final String apiKeyPepper; // from config

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith(ApiPaths.PARTNER + "/me");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // read api key from http header
        String apiKey = request.getHeader("X-API-Key");

        // check if api not null and present if not reject right away
        if (apiKey == null || apiKey.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing X-API-Key");
            return;
        }

        // hash the api
        String hash = sha256(apiKey + apiKeyPepper);

        // look up partner in db and if the partner is active
        var partner = partnerAppRepository.findByApiKeyHashAndStatus(hash, PartnerAppStatus.ACTIVE)
                .orElse(null);

        // if partner not found , access denied
        if (partner == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
            return;
        }

        // create authenticated user (spring security)
        var auth = new UsernamePasswordAuthenticationToken(
                partner.getId(), // principal (partner id)
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PARTNER")));

        // tells spring security , this user is trusted
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
