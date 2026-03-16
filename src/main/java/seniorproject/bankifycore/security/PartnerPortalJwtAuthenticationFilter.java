package seniorproject.bankifycore.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import seniorproject.bankifycore.consants.ApiPaths;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PartnerPortalJwtAuthenticationFilter extends OncePerRequestFilter {


    private final JwtTokenService jwtTokenService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().contains(ApiPaths.PARTNER + "/portal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtTokenService.parseToken(token);

                if (!"PARTNER_PORTAL".equals(String.valueOf(claims.get("typ")))) {
                    filterChain.doFilter(request, response);
                    return;
                }

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UUID partnerUserId = UUID.fromString(claims.getSubject());
                    String role = String.valueOf(claims.get("role")); // OWNER/MEMBER

                    var auth = new UsernamePasswordAuthenticationToken(
                            partnerUserId,
                            null,
                            List.of(
                                    new SimpleGrantedAuthority("ROLE_PARTNER"),
                                    new SimpleGrantedAuthority("ROLE_PARTNER_" + role)
                            )
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }

            } catch (Exception ignored) {
            }
        }

        filterChain.doFilter(request, response);
    }
}
