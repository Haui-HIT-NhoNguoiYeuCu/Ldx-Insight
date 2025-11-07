package io.ldxinsight.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // hỗ trợ @PreAuthorize trên controller
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;              // Filter của bạn để đọc JWT (từ cookie hoặc header)
    private final AuthenticationProvider authenticationProvider; // Provider để validate token & load UserDetails

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS + CSRF
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())

            // Stateless session
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Ủy quyền theo URL
            .authorizeHttpRequests(auth -> auth
                // Preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Swagger (public)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()

                // Auth APIs (public)
                .requestMatchers("/api/v1/auth/**").permitAll()

                // Stats (public)
                .requestMatchers(HttpMethod.GET, "/api/v1/stats/**").permitAll()

                // Dataset: GET public
                .requestMatchers(HttpMethod.GET, "/api/v1/datasets/**").permitAll()

                // Dataset: increment view (public) — SỬA {id} -> * (ant pattern), hoặc ** nếu cần sâu hơn
                .requestMatchers(HttpMethod.POST, "/api/v1/datasets/*/view").permitAll()
                // .requestMatchers(HttpMethod.POST, "/api/v1/datasets/**/view").permitAll() // nếu route có thể lồng sâu

                // Còn lại: cần xác thực (các API ADMIN sẽ bị chặn bởi @PreAuthorize)
                .anyRequest().authenticated()
            )

            // AuthN
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // KHÔNG có dấu "/" cuối cùng
        cfg.setAllowedOrigins(List.of(
            "https://api.haui-hit-h2k.site",
            "http://localhost:3000",
            "http://localhost:5173"
        ));

        cfg.setAllowCredentials(true);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Authorization", "Content-Disposition", "X-Total-Count"));
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
