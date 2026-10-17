package ee.kerrete.ainterview.config;

import lombok.RequiredArgsConstructor;
import ee.kerrete.ainterview.auth.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}")
    private String corsAllowedOrigins;

    /** FP-30: the H2 console is off unless a profile (only application-local.yml) enables it. */
    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Value("${spring.h2.console.path:/h2-console}")
    private String h2ConsolePath;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // kasutame allpool defineeritud corsConfigurationSource()
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Default X-Frame-Options DENY; the H2 console (frames) needs SAMEORIGIN, only when enabled.
                .headers(headers -> headers.frameOptions(frame -> {
                    if (h2ConsoleEnabled) {
                        frame.sameOrigin();
                    } else {
                        frame.deny();
                    }
                }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // CORS preflight peab ALATI läbi minema
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            // FP-60: the full roadmap needs a login; must stay ahead of the public /api/ai-readiness/**
                            .requestMatchers("/api/ai-readiness/roadmap", "/api/ai-readiness/roadmap/**")
                            .hasAnyRole("ADMIN", "USER", "CANDIDATE")
                            .requestMatchers(
                                    "/auth/**",
                                    "/api/auth/**",
                                    "/api/ping",
                                    "/api/ai-readiness/**",
                                    // team report: access comes from the secret report token, not from a login
                                    "/api/teams/**",
                                    "/error"
                            ).permitAll()
                            // FP-62: Stripe calls this without a login; the Stripe-Signature header is the authentication
                            .requestMatchers(HttpMethod.POST, "/api/billing/webhook").permitAll()
                            // FP-30: liveness/readiness for docker/load balancers; status only, no details
                            .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                            // public, server-rendered SEO pages (JobPageController)
                            .requestMatchers(HttpMethod.GET, "/jobs", "/jobs/**", "/sitemap.xml", "/robots.txt").permitAll();
                    if (h2ConsoleEnabled) {
                        auth.requestMatchers(h2ConsolePath + "/**").permitAll();
                    }
                    auth.anyRequest().hasAnyRole("ADMIN", "USER", "CANDIDATE");
                })
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS konfiguratsioon – siit tulevad päised OPTIONS ja POST vastustele
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> allowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();

        if (allowedOrigins.isEmpty()) {
            allowedOrigins = List.of("http://localhost:4200", "http://127.0.0.1:4200");
        }

        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // rakendame KÕIGILE endpointidele
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
