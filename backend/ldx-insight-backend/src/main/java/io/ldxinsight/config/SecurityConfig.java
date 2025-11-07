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
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() 
                        
                        // 1. Cho phép Swagger
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        
                        // =================== LỖI 403 LÀ DO THIẾU DÒNG NÀY ===================
                        // 2. Cho phép API Đăng ký / Đăng nhập / Đăng xuất
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // =====================================================================

                        // 3. Cho phép các API CÔNG KHAI
                        // (Tôi đã rút gọn 5 dòng datasets của bạn thành 3 dòng cho sạch)
                        .requestMatchers(HttpMethod.GET, "/api/v1/datasets/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/datasets/{id}/view").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/stats/**").permitAll() 
                        
                        // 4. Tất cả các API còn lại đều phải xác thực
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        
        // ================== SỬA LỖI CORS (KHÔNG DÙNG "*") ==================
        cfg.setAllowedOrigins(List.of(
            "https://api.haui-hit-h2k.site/", 
            "http://localhost:3000",
            "http://localhost:5173"
        ));
        // ===================================================================
        
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