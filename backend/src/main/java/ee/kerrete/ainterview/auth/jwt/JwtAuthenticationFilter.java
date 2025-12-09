package ee.kerrete.ainterview.auth.jwt;

import ee.kerrete.ainterview.model.UserRole;
import ee.kerrete.ainterview.security.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * JWT authentication filter that validates tokens and sets up security context.
 * Extracts user information from JWT claims - no database lookup required.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
        "/api/auth",
        "/auth",
        "/actuator",
        "/v3/api-docs",
        "/swagger-ui",
        "/h2-console",
        "/error"
    );

    private static final Set<String> PUBLIC_PATH_EXACT = Set.of(
        "/api/ping"
    );

    private final JwtService jwtService;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();

        // Check exact matches first
        if (PUBLIC_PATH_EXACT.contains(path)) {
            return true;
        }

        // Check prefix matches
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        extractBearerToken(request)
            .flatMap(jwtService::parseToken)
            .filter(this::isAccessToken)
            .ifPresent(claims -> authenticateFromClaims(claims, request));

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (!token.isEmpty()) {
                return Optional.of(token);
            }
        }
        return Optional.empty();
    }

    private boolean isAccessToken(Claims claims) {
        String tokenType = claims.get(JwtClaims.TOKEN_TYPE, String.class);
        // For backward compatibility, tokens without type are considered access tokens
        return tokenType == null || JwtClaims.TYPE_ACCESS.equals(tokenType);
    }

    private void authenticateFromClaims(Claims claims, HttpServletRequest request) {
        String email = claims.getSubject();
        String roleName = claims.get(JwtClaims.ROLE, String.class);
        Long userId = claims.get(JwtClaims.USER_ID, Long.class);

        if (email == null || email.isBlank()) {
            log.debug("JWT missing email subject, skipping authentication");
            return;
        }

        // Handle tokens without role/userId (backward compatibility)
        UserRole role = null;
        if (roleName != null && !roleName.isBlank()) {
            try {
                role = UserRole.valueOf(roleName);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role in JWT: {}", roleName);
            }
        }

        // Create authenticated user principal
        AuthenticatedUser principal = new AuthenticatedUser(userId, email, role);

        // Build authorities from role
        List<SimpleGrantedAuthority> authorities = role != null
            ? List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
            : List.of();

        var authentication = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            authorities
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Authenticated user {} with role {}", email, role);
    }
}
